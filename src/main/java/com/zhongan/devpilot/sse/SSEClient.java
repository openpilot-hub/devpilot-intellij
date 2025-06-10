package com.zhongan.devpilot.sse;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.mcp.McpConfigurationHandler;
import com.zhongan.devpilot.session.ChatSessionManagerService;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import static com.zhongan.devpilot.constant.DefaultConst.REMOTE_AGENT_DEFAULT_HOST;
import static com.zhongan.devpilot.constant.DefaultConst.SSE_PATH;

public class SSEClient {

    private static final Logger LOG = Logger.getInstance(SSEClient.class);

    private String clientId = StringUtils.EMPTY;

    private Project project;

    private volatile boolean connected = false;

    private volatile boolean connecting = false;

    private Integer currentPort = null;

    private static final int MAX_RETRY_COUNT = 3;

    private static final long INITIAL_RETRY_INTERVAL = 5000;

    private static final long MAX_RETRY_INTERVAL = 60000;

    private static final long HEARTBEAT_INTERVAL = 30000;

    private int retryCount = 0;

    private long currentRetryInterval = INITIAL_RETRY_INTERVAL;

    private Thread connectionThread;

    private Thread heartbeatThread;

    private volatile long lastMessageTime = System.currentTimeMillis();

    private static final Map<Project, SSEClient> clientInstances = new ConcurrentHashMap<>();

    private SSEClient(Project project) {
        this.project = project;
    }

    public static SSEClient getInstance(Project project) {
        return clientInstances.computeIfAbsent(project, SSEClient::new);
    }

    public static void removeInstance(Project project) {
        SSEClient client = clientInstances.remove(project);
        if (client != null) {
            client.disconnect();
        }
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void connect() {
        if (project == null || project.isDisposed()) {
            return;
        }
        
        if (connectionThread != null && connectionThread.isAlive()) {
            return;
        }

        connectionThread = new Thread(() -> {
            while (retryCount < MAX_RETRY_COUNT && !Thread.currentThread().isInterrupted()) {
                try {
                    Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();
                    if (portPId != null && portPId.first != null) {
                        if (currentPort == null) {
                            currentPort = portPId.first;
                        } else if (!currentPort.equals(portPId.first)) {
                            LOG.info("Port changing from " + currentPort + " to " + portPId.first);
                            disconnect();
                        }
                    }

                    if (connecting) {
                        LOG.info("The connection is being established. Skip this connection request.");
                        return;
                    }
                    connecting = true;

                    if (null != portPId) {
                        String sseUrl = REMOTE_AGENT_DEFAULT_HOST + portPId.first + SSE_PATH;
                        URL url = new URL(sseUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept", "text/event-stream");
                        connection.setDoInput(true);
                        connection.setConnectTimeout(5000);
                        connection.connect();

                        connected = true;
                        retryCount = 0; // Reset retry count on successful connection

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                                lastMessageTime = System.currentTimeMillis();
                                if (line.startsWith("event:")) {
                                    String data = line.substring(6).trim();
                                    processEvent(data);
                                } else if (line.startsWith("id:")) {
                                    lastMessageTime = System.currentTimeMillis();
                                }
                            }
                        } catch (IOException e) {
                            LOG.warn("连接中断: " + e.getMessage(), e);
                            connection.disconnect();
                            throw e;
                        }
                        startHeartbeat();
                    }
                } catch (Exception e) {
                    LOG.warn("Error connecting to SSE server, retry count: " + retryCount, e);
                    retryCount++;
                    try {
                        Thread.sleep(currentRetryInterval);
                        currentRetryInterval = Math.min(currentRetryInterval * 2, MAX_RETRY_INTERVAL);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } finally {
                    connected = false;
                    connecting = false;
                    clientId = StringUtils.EMPTY;
                }
            }
            if (retryCount >= MAX_RETRY_COUNT) {
                LOG.error("连接SSE服务器失败：已达到最大重试次数(" + MAX_RETRY_COUNT + ")");
                ApplicationManager.getApplication().invokeLater(() -> {
                    DevPilotNotification.warn("连接SSE服务器失败");
                });
            }
        });
        connectionThread.start();
    }

    public void shutdown() {
        disconnect();
    }

    private void processEvent(String message) {
        Map<String, String> eventMap = JsonUtils.fromJson(message, Map.class);
        if (null != eventMap) {
            var eventType = eventMap.get("event");
            if (StringUtils.equalsIgnoreCase("ClientConnected", String.valueOf(eventType))) {
                clientId = String.valueOf(eventMap.get("clientId"));
                LOG.warn("Add SSE client: " + clientId + " for " + project.getBasePath());
                project.getService(ChatSessionManagerService.class).getSessionManager().loadSessions(clientId);

                try {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            McpConfigurationHandler.INSTANCE.initialMcpServer();
                        } catch (Exception e) {
                            LOG.warn("Failed to connect to SSE server after SSE server connected", e);
                        }
                    });
                } catch (Exception e) {
                    LOG.warn("Failed to connect to SSE server", e);
                }

            } else if (StringUtils.equalsIgnoreCase("DeepThinking", String.valueOf(eventType))) {
                DeepThinkingEventProcessor.INSTANCE.processDeepThinkingEvent(project, eventMap);
            } else if (StringUtils.equalsIgnoreCase("Session", String.valueOf(eventType))) {
                SessionEventProcessor.INSTANCE.processSessionEvent(project, eventMap);
            } else if (StringUtils.equalsIgnoreCase("McpServers", String.valueOf(eventType))) {
                McpServerEventProcessor.INSTANCE.processMcpServerEvent(project, eventMap);
            }
        }
    }

    public void disconnect() {
        stopHeartbeat();
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
        }
        
        try {
            Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();
            if (null != portPId) {
                String url = REMOTE_AGENT_DEFAULT_HOST + portPId.first + "/remove-sse?clientId=" + clientId;
                var request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", UserAgentUtils.buildUserAgent())
                        .header("Auth-Type", LoginUtils.getLoginType())
                        .get()
                        .build();

                Call call = OkhttpUtils.getClient().newCall(request);
                try (Response response = call.execute()) {
                    LOG.info("Remove SSE client: " + clientId + ", response: " + response);
                }
            }
        } catch (Exception e) {
            LOG.warn("Exception occurred while removing SSE client.", e);
        } finally {
            LOG.warn("Remove SSE client: " + clientId + " for " + project.getBasePath());
            connecting = false;
            connected = false;
            clientId = StringUtils.EMPTY;
            retryCount = 0;
        }
    }

    public String getClientId() {
        return connected ? clientId : StringUtils.EMPTY;
    }

    private void startHeartbeat() {
        if (heartbeatThread != null && heartbeatThread.isAlive()) {
            return;
        }

        heartbeatThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    if (System.currentTimeMillis() - lastMessageTime > HEARTBEAT_INTERVAL * 2) {
                        LOG.warn("No message received for " + (System.currentTimeMillis() - lastMessageTime) + "ms, reconnecting...");
                        disconnect();
                        connect();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        heartbeatThread.start();
    }

    private void stopHeartbeat() {
        if (heartbeatThread != null && heartbeatThread.isAlive()) {
            heartbeatThread.interrupt();
        }
    }
}
