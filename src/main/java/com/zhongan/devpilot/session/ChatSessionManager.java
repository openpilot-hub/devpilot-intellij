package com.zhongan.devpilot.session;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.session.model.ChatSession;
import com.zhongan.devpilot.util.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import static com.zhongan.devpilot.util.EventUtil.sendEventRequest;

/**
 * The plugin maintains memory-level sessions and pushes them to the server for persistence.
 */
public class ChatSessionManager {
    private static final Logger log = Logger.getInstance(ChatSessionManager.class);

    private static final String SESSIONS_DIR = ".session_histories";

    private static final int MAX_SESSIONS = 20;

    private final String basePath;

    private ChatSession currentSession;

    private List<ChatSession> sessions = new ArrayList<>();

    private boolean sessionUpdated = false;

    private String clientId;

    private final Project project;

    private final Object syncLock = new Object();

    public ChatSessionManager(Project project) {
        this.project = project;
        File homeDir = BinaryManager.INSTANCE.getHomeDir();

        this.basePath = homeDir.getPath() + File.separator + SESSIONS_DIR;
        initializeSessionsDirectory();
        loadingSessions();
        if (sessions.isEmpty()) {
            createNewSession();
        } else {
            currentSession = sessions.get(0);
            log.warn("Initial current session by construction. Current session id:" + currentSession.getId() + ".");
        }
        switchSession(currentSession.getId());
    }

    private void initializeSessionsDirectory() {
        File directory = new File(basePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void loadingSessions() {
        File directory = new File(basePath);
        if (directory.exists()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        ChatSession session = JsonUtils.fromJson(json, ChatSession.class);
                        if (session != null) {
                            makeUpForChatMode(session);
                            sessions.add(session);
                        }
                    } catch (IOException e) {
                        sessions.clear();
                    }
                }
            }
        }
        sortSessionsByUpdateTime();
    }

    private void makeUpForChatMode(ChatSession session) {
        if (0 == session.getChatMode()) {
            session.setChatMode(DefaultConst.SMART_CHAT_TYPE);
        }
    }

    private void sortSessionsByUpdateTime() {
        sessions.sort(Comparator.comparing(ChatSession::getUpdateTime).reversed());
    }

    public void createNewSessionWithChatMode(int chatMode) {
        createNewSession();
        currentSession.setChatMode(chatMode);
    }

    public ChatSession createNewSession() {
        if (null != currentSession && CollectionUtils.isEmpty(currentSession.getHistoryRequestMessageList())) {
            log.warn("Since there are no request messages in the current session:" + currentSession.getId() + ", skip creating the session.");
            if (CollectionUtils.isNotEmpty(currentSession.getHistoryMessageList())) {
                clearSession();
            }
            log.warn("Cleared current session:" + currentSession.getId() + " for skipping creating new session.");
            return currentSession;
        }
        while (sessions.size() > MAX_SESSIONS) {
            sortSessionsByUpdateTime();
            ChatSession oldestSession = sessions.get(sessions.size() - 1);
            deleteSession(oldestSession.getId(), Boolean.TRUE);
        }

        ChatSession session = new ChatSession();
        session.setChatMode(null == currentSession ? DefaultConst.AGENT_CHAT_TYPE : currentSession.getChatMode());
        session.setId(UUID.randomUUID().toString());
        session.setCreateTime(System.currentTimeMillis());
        session.setUpdateTime(System.currentTimeMillis());

        sessions.add(session);
        currentSession = session;
        log.warn("Created new session:" + currentSession.getId() + ".");
        return session;

    }

    public void saveSession(ChatSession session) {
        if (session == null || session.getId() == null || CollectionUtils.isEmpty(session.getHistoryRequestMessageList())) {
            log.warn("Skip saving invalided session:" + Optional.ofNullable(session).map(ChatSession::getId).orElse("N/A") + ".");
            return;
        }
        session.setUpdateTime(System.currentTimeMillis());
        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("session", session);
        map.put("sessionDir", basePath);
        publishEvent("Session-Saved", map);
        log.info("Publish Session-Saved event for session:" + session.getId() + " by client:" + clientId + ", current session:" + currentSession.getId() + ".");
    }

    public void deleteSession(String sessionId, boolean directDeleteFlag) {
        sessions.removeIf(s -> s.getId().equals(sessionId));
        if (!directDeleteFlag && (currentSession != null && currentSession.getId().equals(sessionId))) {
            currentSession = sessions.isEmpty() ? createNewSession() : getLastModifiedSession();
            log.warn("Deleted session:" + sessionId + ", and switched to session:" + currentSession.getId() + ".");
        }

        // 发请求通知Node Agent处理clientSessionMap以及memoryMap
        Map<String, Object> map = buildClientSessionMap();
        map.put("deletedSessionId", sessionId);
        publishEvent("Session-Deleted", map);
        log.info("Publish Session-Deleted event for deleted session:" + sessionId + " by client:" + clientId + ", current session:" + currentSession.getId() + ".");
    }

    public void switchSession(String sessionId) {
        for (ChatSession session : sessions) {
            if (session.getId().equals(sessionId)) {
                currentSession = session;
                log.warn("Switched to session:" + currentSession.getId() + ".");
                break;
            }
        }
        Map<String, Object> map = buildClientSessionMap();
        map.put("currentVersion", System.currentTimeMillis());
        publishEvent("Session-Switched", map);
        log.info("Publish Session-Switched event for session:" + currentSession.getId() + " by client:" + clientId + ".");
    }

    private Map<String, Object> buildClientSessionMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("sessionId", currentSession.getId());
        map.put("sessionDir", basePath);
        return map;
    }

    public ChatSession getCurrentSession() {
        return currentSession;
    }

    private ChatSession getLastModifiedSession() {
        sortSessionsByUpdateTime();
        return sessions.get(0);
    }

    /**
     * Retrieve sessions for rendering history.
     *
     * @return ChatSession list
     */
    public List<ChatSession> getSessions() {
        List<ChatSession> remoteSessions = LlmProviderFactory.INSTANCE.getLlmProvider(this.project).retrieveSessions(buildClientSessionMap());
        if (CollectionUtils.isNotEmpty(remoteSessions)) {
            sessions = remoteSessions;
        }
        sortSessionsByUpdateTime();
        return new ArrayList<>(sessions);
    }

    public boolean isSessionUpdated() {
        return sessionUpdated;
    }

    public void setSessionUpdated(boolean sessionUpdated) {
        this.sessionUpdated = sessionUpdated;
    }

    public void deleteMessage(String id) {
        currentSession.setUpdateTime(System.currentTimeMillis());
        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("session", currentSession);
        map.put("sessionDir", basePath);
        map.put("deletedId", id);
        publishEvent("Session-Delete-Message", map);
    }

    public void sessionUIRefreshed() {
        if (CollectionUtils.isNotEmpty(currentSession.getHistoryMessageList())) {
            Map<String, Object> map = new HashMap<>();
            map.put("clientId", clientId);
            map.put("session", currentSession);
            map.put("sessionDir", basePath);
            publishEvent("Session-UI-Refreshed", map);
            log.info("Publish Session-UI-Refreshed event for session:" + currentSession.getId() + " by client:" + clientId + ".");
        }
    }

    public void handleRequestMessageListSaved(ChatSession eventSession) {
        if (null != eventSession && null != currentSession && StringUtils.equalsIgnoreCase(currentSession.getId(), eventSession.getId())) {
            currentSession.setHistoryRequestMessageList(eventSession.getHistoryRequestMessageList());
            currentSession.setContainsRequireToolPrompts(eventSession.isContainsRequireToolPrompts());
            currentSession.setContainsRequireResourcePrompts(eventSession.isContainsRequireResourcePrompts());
            currentSession.setContainsRequirePromptsPrompts(eventSession.isContainsRequirePromptsPrompts());
            log.info("Session request message list updated for session:" + currentSession.getId() + ".");
            sessionUIRefreshed();
        }
    }

    public void clearSession() {
        currentSession.getHistoryMessageList().clear();
        currentSession.getHistoryRequestMessageList().clear();
        currentSession.setAbort(Boolean.FALSE);
        currentSession.setContainsRequireToolPrompts(Boolean.FALSE);
        currentSession.setContainsRequireResourcePrompts(Boolean.FALSE);
        currentSession.setContainsRequirePromptsPrompts(Boolean.FALSE);
        currentSession.setUpdateTime(System.currentTimeMillis());
        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("session", currentSession);
        map.put("sessionDir", basePath);
        publishEvent("Session-Cleared", map);
        log.info("Cleared Session-Cleared event for session:" + currentSession.getId() + " by client:" + clientId + ".");

    }

    /**
     * Load sessions after sse connected.
     *
     * @param clientId sse client ID
     */
    public void loadSessions(String clientId) {
        synchronized (syncLock) {
            this.clientId = clientId;
            sessions = LlmProviderFactory.INSTANCE.getLlmProvider(this.project).retrieveSessions(buildClientSessionMap());

            if (null == currentSession) {
                if (sessions.isEmpty()) {
                    createNewSession();
                } else {
                    currentSession = sessions.get(0);
                    log.warn("Loaded session:" + currentSession.getId() + ".");
                }
            } else {
                if (sessions.stream().noneMatch(session -> session.getId().equals(currentSession.getId()))) {
                    createNewSession();
                }
            }
            switchSession(currentSession.getId());
        }
    }

    private void publishEvent(String event, Object data) {
        try {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                log.info("Publish session event.");
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("eventType", event);
                    body.put("data", data);
                    sendEventRequest(body);
                } catch (Throwable e) {
                    log.warn("Exception occurred while handling sending session event.", e);
                }
            });
        } catch (ProcessCanceledException e) {
            log.warn("Process canceled while handling sending session event:" + event + ".", e);
        }
    }

    public String getSessionsDir() {
        return basePath;
    }
}
