package com.zhongan.devpilot.session;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.session.model.ChatSession;
import com.zhongan.devpilot.util.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;

public class ChatSessionManager {
    private static final Logger log = Logger.getInstance(ChatSessionManager.class);

    private static final String SESSIONS_DIR = ".session_histories";

    private static final int MAX_SESSIONS = 20;

    private final String basePath;

    private ChatSession currentSession;

    private final List<ChatSession> sessions = new ArrayList<>();

    public ChatSessionManager(Project project) {
        File homeDir = BinaryManager.INSTANCE.getHomeDir();

        this.basePath = homeDir.getPath() + File.separator + SESSIONS_DIR;
        initializeSessionsDirectory();
        loadSessions();
        if (sessions.isEmpty()) {
            createNewSession();
        } else {
            currentSession = sessions.get(0);
        }
    }

    private void initializeSessionsDirectory() {
        File directory = new File(basePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void loadSessions() {
        File directory = new File(basePath);
        if (directory.exists()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        ChatSession session = JsonUtils.fromJson(json, ChatSession.class);
                        if (session != null) {
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

    private void sortSessionsByUpdateTime() {
        sessions.sort(Comparator.comparing(ChatSession::getUpdateTime).reversed());
    }

    public ChatSession createNewSession() {
        if (null != currentSession && CollectionUtils.isEmpty(currentSession.getHistoryRequestMessageList())) {
            return currentSession;
        }
        while (sessions.size() > MAX_SESSIONS) {
            sortSessionsByUpdateTime();
            ChatSession oldestSession = sessions.get(sessions.size() - 1);
            deleteSession(oldestSession.getId());
        }

        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setCreateTime(System.currentTimeMillis());
        session.setUpdateTime(System.currentTimeMillis());

        sessions.add(session);
        currentSession = session;
        return session;
    }

    public void saveSession(ChatSession session) {
        if (session == null || session.getId() == null || CollectionUtils.isEmpty(session.getHistoryRequestMessageList())) {
            log.warn("Skip saving invalided session.");
            return;
        }

        session.setUpdateTime(System.currentTimeMillis());
        String json = JsonUtils.toJson(session);
        if (json != null) {
            try {
                File file = new File(basePath + File.separator + session.getId() + ".json");
                FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("Exception occurred while saving session.", e);
            }
        }
    }

    public void deleteSession(String sessionId) {
        sessions.removeIf(s -> s.getId().equals(sessionId));
        File file = new File(basePath + File.separator + sessionId + ".json");
        if (file.exists()) {
            file.delete();
        }

        if (currentSession != null && currentSession.getId().equals(sessionId)) {
            currentSession = sessions.isEmpty() ? createNewSession() : getLastModifiedSession();
        }
    }

    public void switchSession(String sessionId) {
        for (ChatSession session : sessions) {
            if (session.getId().equals(sessionId)) {
                currentSession = session;
                break;
            }
        }
    }

    public ChatSession getCurrentSession() {
        return currentSession;
    }

    private ChatSession getLastModifiedSession() {
        sortSessionsByUpdateTime();
        return sessions.get(0);
    }

    public List<ChatSession> getSessions() {
        sortSessionsByUpdateTime();
        return new ArrayList<>(sessions);
    }

}
