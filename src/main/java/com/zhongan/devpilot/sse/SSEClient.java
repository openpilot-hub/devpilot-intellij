package com.zhongan.devpilot.sse;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.agents.AgentRefreshedObserver;
import com.zhongan.devpilot.agents.AgentsRunner;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.zhongan.devpilot.constant.DefaultConst.REMOTE_AGENT_DEFAULT_HOST;
import static com.zhongan.devpilot.constant.DefaultConst.SSE_PATH;
import static com.zhongan.devpilot.util.ProjectUtil.getProjectIdentifier;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class SSEClient implements AgentRefreshedObserver {

    private static final Logger LOG = Logger.getInstance(SSEClient.class);

    private volatile String clientId = StringUtils.EMPTY;

    private final Project project;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final AtomicBoolean connecting = new AtomicBoolean(false);

    private volatile Integer currentPort = null;

    private static final int MAX_RETRY_COUNT = 5;

    private static final long INITIAL_RETRY_INTERVAL = 5000;

    private static final long MAX_RETRY_INTERVAL = 60000;

    private static final long HEARTBEAT_INTERVAL = 30000;

    private static final int CONNECTION_TIMEOUT = 5000;

    private static final int MAX_CONSECUTIVE_ERRORS = 5; // 心跳线程连续错误最大次数

    private final AtomicInteger retryCount = new AtomicInteger(0);

    private final AtomicInteger consecutiveErrorCount = new AtomicInteger(0); // 连续错误计数

    private final AtomicLong currentRetryInterval = new AtomicLong(INITIAL_RETRY_INTERVAL);

    private volatile Thread connectionThread;

    private volatile Thread heartbeatThread;

    private volatile long lastMessageTime = System.currentTimeMillis();

    private static final Map<Project, SSEClient> clientInstances = new ConcurrentHashMap<>();

    private final ReentrantLock connectionLock = new ReentrantLock();

    private final ReentrantLock heartbeatLock = new ReentrantLock();

    private ScheduledExecutorService heartbeatExecutor;

    private enum ConnectionErrorType {
        NETWORK_ERROR,
        AGENT_NOT_RUNNING,
        INVALID_PORT,
        UNKNOWN_ERROR
    }

    private SSEClient(Project project) {
        this.project = project;
    }

    public static SSEClient getInstance(Project project) {
        return clientInstances.computeIfAbsent(project, SSEClient::new);
    }

    @Override
    public void onRefresh() {
        if (!sendPingMessage()) {
            disconnect();
            connect();
        }
    }

    public static void removeInstance(Project project) {
        SSEClient client = clientInstances.remove(project);
        if (client != null) {
            AgentsRunner.INSTANCE.removeRefreshObserver(client);
            client.disconnect();
        }
    }

    public void connect() {
        if (project == null || project.isDisposed()) {
            LOG.warn("无法连接：项目为空或已销毁");
            return;
        }

        if (connectionThread != null && connectionThread.isAlive()) {
            LOG.info("连接线程已存在，跳过本次连接请求");
            return;
        }

        // 重置重试计数和间隔
        retryCount.set(0);
        currentRetryInterval.set(INITIAL_RETRY_INTERVAL);


        connectionThread = new Thread(() -> {
            while (retryCount.get() < MAX_RETRY_COUNT && !Thread.currentThread().isInterrupted()) {
                HttpURLConnection connection = null;
                try {
                    Pair<Integer, Long> portPId = getAgentPort();
                    if (portPId == null) {
                        handleConnectionError(ConnectionErrorType.AGENT_NOT_RUNNING);
                        continue;
                    }

                    if (!validateAndUpdatePort(portPId)) {
                        continue;
                    }

                    if (!trySetConnecting()) {
                        return;
                    }

                    connection = establishConnection();
                    if (connection == null) {
                        handleConnectionError(ConnectionErrorType.NETWORK_ERROR);
                        continue;
                    }

                    connected.set(true);
                    retryCount.set(0);

                    processEventStream(connection);

                } catch (IOException e) {
                    LOG.warn("SSE连接IO异常: " + e.getMessage(), e);
                    handleConnectionError(ConnectionErrorType.NETWORK_ERROR);
                } catch (Throwable e) {
                    LOG.warn("SSE连接异常: " + e.getMessage(), e);
                    handleConnectionError(ConnectionErrorType.UNKNOWN_ERROR);
                } finally {
                    resetConnectionState();
                    if (connection != null) {
                        try {
                            connection.disconnect();
                        } catch (Exception e) {
                            LOG.warn("关闭连接时发生异常", e);
                        }
                    }
                }
            }

            if (retryCount.get() >= MAX_RETRY_COUNT) {
                LOG.error("连接SSE服务器失败：已达到最大重试次数(" + MAX_RETRY_COUNT + ")，最后使用的端口: " + currentPort);
                ApplicationManager.getApplication().invokeLater(() ->
                    DevPilotNotification.warn("连接SSE服务器失败，请检查网络或重启IDE")
                );
            }
        });

        connectionThread.setName("SSE-Connection-Thread-" + System.currentTimeMillis());
        connectionThread.start();
    }

    private Pair<Integer, Long> getAgentPort() {
        Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();

        if (portPId == null || portPId.first == null) {
            LOG.info("Agent未运行或端口信息不可用，尝试重新启动Agent");
            return null;
        }

        return portPId;
    }

    private boolean validateAndUpdatePort(Pair<Integer, Long> portPId) {
        boolean lockAcquired = false;

        try {
            lockAcquired = connectionLock.tryLock(5, TimeUnit.SECONDS); // 添加锁获取超时
            if (!lockAcquired) {
                LOG.warn("获取连接锁超时，跳过本次端口验证");
                return false;
            }

            if (portPId.first <= 0 || portPId.first > 65535) {
                LOG.warn("获取到不合法的端口: " + portPId.first + "，跳过本次连接尝试");
                Thread.sleep(currentRetryInterval.get());
                return false;
            }

            if (currentPort != null && !currentPort.equals(portPId.first)) {
                LOG.info("端口变更: " + currentPort + " -> " + portPId.first + "，断开旧连接");
                boolean needDisconnect = connected.get() || connecting.get();
                if (needDisconnect) {
                    disconnect();
                } else {
                    LOG.info("旧连接已断开，无需再次断开");
                    resetConnectionState();
                }
            }

            currentPort = portPId.first;
            LOG.info("当前使用端口: " + currentPort);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lockAcquired) {
                connectionLock.unlock();
            }
        }
    }

    private boolean trySetConnecting() {
        boolean lockAcquired = false;
        try {
            lockAcquired = connectionLock.tryLock(5, TimeUnit.SECONDS);
            if (!lockAcquired) {
                LOG.warn("获取连接锁超时，跳过连接设置");
                return false;
            }

            if (connecting.get()) {
                LOG.info("连接正在建立中，跳过本次连接请求,端口是：" + currentPort + ".");
                return false;
            }
            connecting.set(true);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lockAcquired) {
                connectionLock.unlock();
            }
        }
    }

    private HttpURLConnection establishConnection() {
        try {
            String sseUrl = REMOTE_AGENT_DEFAULT_HOST + currentPort + SSE_PATH;
            LOG.warn("尝试连接SSE服务: " + sseUrl);

            URL url = new URL(sseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setDoInput(true);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                LOG.warn("SSE连接失败，响应码: " + responseCode);
                safeDisconnect(connection);
                return null;
            }

            return connection;
        } catch (IOException e) {
            LOG.warn("建立SSE连接失败: " + e.getMessage(), e);
            return null;
        }
    }

    private void safeDisconnect(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                LOG.warn("安全断开连接时发生异常", e);
            }
        }
    }

    private void processEventStream(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("event:")) {
                    lastMessageTime = System.currentTimeMillis();
                    String data = line.substring(6).trim();
                    processEvent(data);
                } else if (line.startsWith("id:")) {
                    lastMessageTime = System.currentTimeMillis();
                } else if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if (!data.isEmpty()) {
                        lastMessageTime = System.currentTimeMillis();
                        LOG.debug("收到SSE数据: " + data);
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("SSE事件流处理中断: " + e.getMessage(), e);
            throw e;
        }
    }

    private void handleConnectionError(ConnectionErrorType errorType) {
        int count = retryCount.incrementAndGet();
        connecting.set(false);

        String errorMessage;
        switch (errorType) {
            case NETWORK_ERROR:
                errorMessage = "网络连接错误";
                break;
            case AGENT_NOT_RUNNING:
                errorMessage = "Agent未运行";
                break;
            case INVALID_PORT:
                errorMessage = "无效端口";
                break;
            default:
                errorMessage = "未知错误";
        }

        LOG.warn("SSE连接失败(" + errorMessage + ")，重试次数: " + count + " / " + MAX_RETRY_COUNT + ", 当前端口: " + currentPort);

        try {
            Thread.sleep(currentRetryInterval.get());
            currentRetryInterval.set(Math.min(currentRetryInterval.get() * 2, MAX_RETRY_INTERVAL));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }



    public void shutdown() {
        disconnect();
    }

    private void processEvent(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }

        try {
            Map<String, String> eventMap = JsonUtils.fromJson(message, Map.class);
            if (null != eventMap) {
                var eventType = eventMap.get("event");
                if (StringUtils.equalsIgnoreCase("ClientConnected", String.valueOf(eventType))) {
                    handleClientConnectedEvent(eventMap);
                } else if (StringUtils.equalsIgnoreCase("DeepThinking", String.valueOf(eventType))) {
                    DeepThinkingEventProcessor.INSTANCE.processDeepThinkingEvent(project, eventMap);
                } else if (StringUtils.equalsIgnoreCase("Session", String.valueOf(eventType))) {
                    SessionEventProcessor.INSTANCE.processSessionEvent(project, eventMap);
                } else if (StringUtils.equalsIgnoreCase("McpServers", String.valueOf(eventType))) {
                    McpServerEventProcessor.INSTANCE.processMcpServerEvent(project, eventMap);
                } else {
                    LOG.warn("收到未知类型事件: " + eventType);
                }
            }
        } catch (Throwable e) {
            LOG.warn("处理SSE事件时发生异常: " + e.getMessage(), e);
        }
    }


    private void handleClientConnectedEvent(Map<String, String> eventMap) {
        boolean lockAcquired = false;

        try {
            lockAcquired = connectionLock.tryLock(5, TimeUnit.SECONDS);
            if (!lockAcquired) {
                LOG.warn("获取连接锁超时，无法处理客户端连接事件");
                return;
            }
            clientId = String.valueOf(eventMap.get("clientId"));
            LOG.info("添加SSE客户端: " + clientId + " 项目路径: " + project.getBasePath());

            if (project.isDisposed()) {
                LOG.warn("项目已销毁，跳过会话加载");
                return;
            }

            startHeartbeat();

            project.getService(ChatSessionManagerService.class).getSessionManager().loadSessions(clientId);

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    McpConfigurationHandler.INSTANCE.initialMcpServer();
                } catch (Exception e) {
                    LOG.warn("SSE连接后初始化MCP服务器失败", e);
                }
            });
        } catch (Throwable e) {
            LOG.warn("处理客户端连接事件时发生异常", e);
        } finally {
            if (lockAcquired) {
                connectionLock.unlock();
            }
        }
    }

    public void disconnect() {
        stopHeartbeat();

        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
            try {
                connectionThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        removeClientFromServer();
        String logMessage = "断开SSE连接: " + clientId + " 项目路径: " + (project != null ? project.getBasePath() : "未知");
        resetConnectionState(true, logMessage);
    }

    private void resetConnectionState() {
        resetConnectionState(false, null);

    }

    private void resetConnectionState(boolean resetRetryCount, String logMessage) {
        boolean lockAcquired = false;
        try {
            lockAcquired = connectionLock.tryLock(5, TimeUnit.SECONDS);
            if (!lockAcquired) {
                LOG.warn("获取连接锁超时，无法重置连接状态");
                connected.set(false);
                connecting.set(false);
                return;
            }

            if (logMessage != null) {
                LOG.info(logMessage);
            }

            connecting.set(false);
            connected.set(false);
            clientId = StringUtils.EMPTY;

            if (resetRetryCount) {
                retryCount.set(0);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lockAcquired) {
                connectionLock.unlock();
            }
        }
    }

    private void removeClientFromServer() {
        String currentClientId;
        boolean lockAcquired = false;
        try {
            lockAcquired = connectionLock.tryLock(5, TimeUnit.SECONDS);
            if (!lockAcquired) {
                LOG.warn("获取连接锁超时，无法获取客户端ID");
                return;
            }
            currentClientId = clientId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } finally {
            if (lockAcquired) {
                connectionLock.unlock();
            }
        }

        if (StringUtils.isEmpty(currentClientId)) {
            LOG.info("客户端ID为空，无需移除SSE客户端");
            return;
        }

        try {
            Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();
            if (portPId != null && portPId.first != null && portPId.first > 0 && portPId.first <= 65535) {
                String url = REMOTE_AGENT_DEFAULT_HOST + portPId.first + "/remove-sse?clientId=" + currentClientId;
                LOG.info("尝试移除SSE客户端: " + url);

                var request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", UserAgentUtils.buildUserAgent())
                        .header("Auth-Type", LoginUtils.getLoginType())
                        .get()
                        .build();

                Call call = OkhttpUtils.getClient().newCall(request);
                try (Response response = call.execute()) {
                    if (response.isSuccessful()) {
                        LOG.info("成功移除SSE客户端: " + currentClientId);
                    } else {
                        LOG.warn("移除SSE客户端失败: " + currentClientId + ", 响应码: " + response.code());
                    }
                }
            } else {
                LOG.warn("无法获取有效端口或端口不合法，跳过移除SSE客户端请求");
            }
        } catch (Exception e) {
            LOG.warn("移除SSE客户端过程中发生异常: " + e.getMessage(), e);
        }
    }

    public String getClientId() {
        return connected.get() ? clientId : StringUtils.EMPTY;
    }

    private void startHeartbeat() {
        boolean lockAcquired = false;
        try {
            lockAcquired = heartbeatLock.tryLock(5, TimeUnit.SECONDS);
            if (!lockAcquired) {
                LOG.warn("获取心跳锁超时，无法启动心跳线程");
                return;
            }

            if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
                LOG.warn("心跳执行器已在运行中...");
                return;
            }

            heartbeatExecutor = newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "SSE-Heartbeat-Executor-" + System.currentTimeMillis());
                thread.setDaemon(true);
                return thread;
            });

            LOG.info("心跳执行器已启动");
            heartbeatExecutor.scheduleAtFixedRate(() -> {
                try {
                    String projectIdentifier = getProjectIdentifier(project);
                    LOG.info("项目[" + projectIdentifier + "] 判断是否需要发送ping消息...");
                    long timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime;

                    if (timeSinceLastMessage > HEARTBEAT_INTERVAL) {
                        if (sendPingMessage()) {
                            lastMessageTime = System.currentTimeMillis();
                            LOG.info("项目[" + projectIdentifier + "] 发送ping消息成功...");
                            consecutiveErrorCount.set(0);
                        } else {
                            int errors = consecutiveErrorCount.incrementAndGet();

                            LOG.warn("项目[" + projectIdentifier + "] ping消息发送失败，重新连接...");
                            if (errors >= MAX_CONSECUTIVE_ERRORS) {
                                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                                    disconnect();
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                    }
                                    connect();
                                });
                                heartbeatExecutor.shutdown();
                            }
                        }
                    }
                } catch (Throwable e) {
                    LOG.warn("心跳任务发生异常: " + e.getMessage(), e);
                    int errors = consecutiveErrorCount.incrementAndGet();
                    if (errors >= MAX_CONSECUTIVE_ERRORS) {
                        LOG.warn("心跳任务异常次数过多，终止心跳");
                        heartbeatExecutor.shutdown();
                    } else {
                        LOG.info("心跳任务尝试从异常中恢复，连续错误次数: " + errors);
                    }
                }
            }, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lockAcquired) {
                heartbeatLock.unlock();
            }
        }
    }

    private void stopHeartbeat() {
        boolean lockAcquired = false;
        try {
            lockAcquired = heartbeatLock.tryLock(5, TimeUnit.SECONDS);
            if (!lockAcquired) {
                LOG.warn("获取心跳锁超时，无法完全停止心跳执行器");
                if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
                    heartbeatExecutor.shutdownNow();
                }
                return;
            }

            if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
                heartbeatExecutor.shutdownNow();
                try {
                    if (!heartbeatExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                        LOG.warn("心跳执行器未能在1秒内完全终止");
                    } else {
                        LOG.info("心跳执行器已成功终止");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warn("等待心跳执行器终止时被中断");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lockAcquired) {
                heartbeatLock.unlock();
            }
        }
    }

    private boolean sendPingMessage() {
        try {
            if (currentPort == null || StringUtils.isEmpty(clientId)) {
                return false;
            }

            String pingUrl = REMOTE_AGENT_DEFAULT_HOST + currentPort + "/ping?clientId=" + clientId;
            LOG.debug("发送ping消息: " + pingUrl);

            Request request = new Request.Builder()
                    .url(pingUrl)
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .header("Auth-Type", LoginUtils.getLoginType())
                    .get()
                    .build();

            Call call = OkhttpUtils.getClient().newCall(request);
            try (Response response = call.execute()) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String responseBodyString = responseBody.string();
                    LOG.info("Getting ping response: " + responseBodyString + ", request is: " + response.isSuccessful() + ".");
                }
                return response.isSuccessful();
            }
        } catch (Throwable e) {
            LOG.warn("发送ping消息异常: " + e.getMessage(), e);
            return false;
        }
    }

}