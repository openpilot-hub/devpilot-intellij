package com.zhongan.devpilot.sse;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.concurrency.AppExecutorUtil;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.zhongan.devpilot.constant.DefaultConst.REMOTE_AGENT_DEFAULT_HOST;
import static com.zhongan.devpilot.constant.DefaultConst.SSE_PATH;
import static com.zhongan.devpilot.util.ProjectUtil.getProjectIdentifier;

public class SSEClient implements AgentRefreshedObserver {

    private static final Logger LOG = Logger.getInstance(SSEClient.class);


    // 连接状态枚举
    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        FAILED
    }

    // 连接错误类型枚举
    private enum ConnectionErrorType {
        NETWORK_ERROR,
        AGENT_NOT_RUNNING,
        INVALID_PORT,
        UNKNOWN_ERROR
    }

    // 事件处理器接口
    private interface EventProcessor {
        void process(Project project, Map<String, String> eventData);
    }

    private volatile String clientId = StringUtils.EMPTY;
    private final Project project;
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private volatile Integer currentPort = null;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private volatile long currentRetryInterval;
    private final AtomicBoolean shouldBeActive = new AtomicBoolean(false);
    private volatile long lastMessageTime = System.currentTimeMillis();

    // 线程和执行器
    private final ExecutorService threadPool;
    private final ScheduledExecutorService heartbeatScheduler;
    private final CopyOnWriteArrayList<Future<?>> connectionFutures = new CopyOnWriteArrayList<>();

    // 锁
    private final ReentrantLock connectionLock = new ReentrantLock();
    private final ReentrantLock heartbeatLock = new ReentrantLock();

    // 配置
    private Config config;

    // 事件处理器映射
    private final Map<String, EventProcessor> eventProcessors = new HashMap<>();

    // 客户端实例管理
    private static final Map<Project, SSEClient> clientInstances = Collections.synchronizedMap(new WeakHashMap<>());


    // 配置类
    public static class Config {
        // 默认值
        private static final int DEFAULT_MAX_RETRY_COUNT = 5;
        private static final long DEFAULT_INITIAL_RETRY_INTERVAL = 5000;
        private static final long DEFAULT_MAX_RETRY_INTERVAL = 60000;
        private static final long DEFAULT_HEARTBEAT_INTERVAL = 30000;
        private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
        private static final long DEFAULT_HEARTBEAT_MONITOR_INTERVAL = 60000;

        // 可配置参数
        private int maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
        private long initialRetryInterval = DEFAULT_INITIAL_RETRY_INTERVAL;
        private long maxRetryInterval = DEFAULT_MAX_RETRY_INTERVAL;
        private long heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private long heartbeatMonitorInterval = DEFAULT_HEARTBEAT_MONITOR_INTERVAL;

        public Config() {
        }

        public Config(int maxRetryCount, long initialRetryInterval, long maxRetryInterval,
                      long heartbeatInterval, int connectionTimeout) {
            this.maxRetryCount = maxRetryCount;
            this.initialRetryInterval = initialRetryInterval;
            this.maxRetryInterval = maxRetryInterval;
            this.heartbeatInterval = heartbeatInterval;
            this.connectionTimeout = connectionTimeout;
        }

        // Getters and setters
        public int getMaxRetryCount() {
            return maxRetryCount;
        }

        public void setMaxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
        }

        public long getInitialRetryInterval() {
            return initialRetryInterval;
        }

        public void setInitialRetryInterval(long initialRetryInterval) {
            this.initialRetryInterval = initialRetryInterval;
        }

        public long getMaxRetryInterval() {
            return maxRetryInterval;
        }

        public void setMaxRetryInterval(long maxRetryInterval) {
            this.maxRetryInterval = maxRetryInterval;
        }

        public long getHeartbeatInterval() {
            return heartbeatInterval;
        }

        public void setHeartbeatInterval(long heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getHeartbeatMonitorInterval() {
            return heartbeatMonitorInterval;
        }

        public void setHeartbeatMonitorInterval(long heartbeatMonitorInterval) {
            this.heartbeatMonitorInterval = heartbeatMonitorInterval;
        }
    }

    private SSEClient(@NotNull Project project, @Nullable Config config) {
        this.project = project;
        this.config = config != null ? config : new Config();
        this.currentRetryInterval = this.config.initialRetryInterval;

        // 创建线程池
        this.threadPool = AppExecutorUtil.createBoundedApplicationPoolExecutor(
                "SSE-Thread-Pool", 5
        );

        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("SSE-Heartbeat-%d").setDaemon(true).build()
        );

        // 注册事件处理器
        registerEventProcessors();
    }

    private void registerEventProcessors() {
        eventProcessors.put("ClientConnected", this::handleClientConnectedEvent);
        eventProcessors.put("DeepThinking", (p, data) -> DeepThinkingEventProcessor.INSTANCE.processDeepThinkingEvent(p, data));
        eventProcessors.put("Session", (p, data) -> SessionEventProcessor.INSTANCE.processSessionEvent(p, data));
        eventProcessors.put("McpServers", (p, data) -> McpServerEventProcessor.INSTANCE.processMcpServerEvent(p, data));
        eventProcessors.put("Pong", (p, data) -> handlePongEvent());
    }

    public static SSEClient getInstance(@NotNull Project project) {
        return clientInstances.computeIfAbsent(project, p -> new SSEClient(p, new Config()));
    }

    public static SSEClient getInstance(@NotNull Project project, @Nullable Config config) {
        return clientInstances.computeIfAbsent(project, p -> new SSEClient(p, config));
    }

    public static void removeInstance(@NotNull Project project) {
        SSEClient client = clientInstances.remove(project);
        if (client != null) {
            AgentsRunner.INSTANCE.removeRefreshObserver(client);
            client.disconnect();
        }
        cleanupInstances();
    }

    public static void cleanupInstances() {
        synchronized (clientInstances) {
            Iterator<Map.Entry<Project, SSEClient>> iterator = clientInstances.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Project, SSEClient> entry = iterator.next();
                Project project = entry.getKey();
                if (project == null || project.isDisposed()) {
                    SSEClient client = entry.getValue();
                    if (client != null) {
                        client.disconnect();
                    }
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void onRefresh() {
        if (!sendPingMessage()) {
            reconnect();
        }
    }

    public void connect() {
        if (project == null || project.isDisposed()) {
            LOG.warn("无法连接：项目为空或已销毁");
            return;
        }

        if (!transitionState(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING)) {
            LOG.info("连接已在进行中或已连接，跳过本次连接请求");
            return;
        }

        shouldBeActive.set(true);
        retryCount.set(0);
        currentRetryInterval = config.initialRetryInterval;

        Future<?> future = threadPool.submit(this::connectionTask);
        connectionFutures.add(future);
    }

    private void connectionTask() {
        String projectIdentifier = getProjectIdentifier(project);
        LOG.info("开始为项目[" + projectIdentifier + "]建立SSE连接");

        while (retryCount.get() < config.maxRetryCount && !Thread.currentThread().isInterrupted() && shouldBeActive.get()) {
            HttpURLConnection connection = null;
            try {
                logConnectionAttempt();

                Pair<Integer, Long> portPId = getAgentPort();
                if (portPId == null) {
                    handleConnectionError(ConnectionErrorType.AGENT_NOT_RUNNING);
                    continue;
                }

                if (!validateAndUpdatePort(portPId)) {
                    continue;
                }

                connection = establishConnection();
                if (connection == null) {
                    handleConnectionError(ConnectionErrorType.NETWORK_ERROR);
                    continue;
                }

                transitionState(ConnectionState.CONNECTING, ConnectionState.CONNECTED);
                retryCount.set(0);
                lastMessageTime = System.currentTimeMillis();

                processEventStream(connection);

            } catch (IOException e) {
                LOG.warn("SSE连接IO异常: " + e.getMessage(), e);
                handleConnectionError(ConnectionErrorType.NETWORK_ERROR);
            } catch (Throwable e) {
                LOG.warn("SSE连接异常: " + e.getMessage(), e);
                handleConnectionError(ConnectionErrorType.UNKNOWN_ERROR);
            } finally {
                resetConnectionState();
                closeQuietly(connection);
            }
        }

        if (retryCount.get() >= config.maxRetryCount && shouldBeActive.get()) {
            LOG.error("连接SSE服务器失败：已达到最大重试次数(" + config.maxRetryCount + ")，最后使用的端口: " + currentPort);
            ApplicationManager.getApplication().invokeLater(() -> {
                DevPilotNotification.warn("连接SSE服务器失败，请检查网络或重启IDE");
            });
        }
    }

    private void logConnectionAttempt() {
        String projectIdentifier = getProjectIdentifier(project);
        LOG.info("尝试为项目[" + projectIdentifier + "]建立SSE连接，端口: " + currentPort +
                ", 重试次数: " + retryCount.get() + "/" + config.maxRetryCount);
    }

    private Pair<Integer, Long> getAgentPort() {
        Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();

        if (portPId == null || portPId.first == null) {
            LOG.info("Agent未运行或端口信息不可用，尝试重新启动Agent");
            try {
                // 等待一段时间后再次尝试获取端口
                Thread.sleep(3000);
                return BinaryManager.INSTANCE.retrieveAlivePort();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return portPId;
    }

    private boolean validateAndUpdatePort(Pair<Integer, Long> portPId) {
        try {
            connectionLock.lock();

            if (portPId.first <= 0 || portPId.first > 65535) {
                LOG.warn("获取到不合法的端口: " + portPId.first + "，跳过本次连接尝试");
                Thread.sleep(currentRetryInterval);
                return false;
            }

            if (currentPort != null && !currentPort.equals(portPId.first)) {
                LOG.info("端口变更: " + currentPort + " -> " + portPId.first + "，断开旧连接");
                boolean needDisconnect = isConnected() || isConnecting();
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
            connectionLock.unlock();
        }
    }

    private HttpURLConnection establishConnection() {
        HttpURLConnection connection = null;
        try {
            String sseUrl = REMOTE_AGENT_DEFAULT_HOST + currentPort + SSE_PATH;
            LOG.info("尝试连接SSE服务: " + sseUrl);

            URL url = new URL(sseUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("User-Agent", UserAgentUtils.buildUserAgent());
            connection.setRequestProperty("Auth-Type", LoginUtils.getLoginType());
            connection.setDoInput(true);
            connection.setConnectTimeout(config.connectionTimeout);
            connection.setReadTimeout(0); // 无限读取超时
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                LOG.warn("SSE连接失败，响应码: " + responseCode);
                closeQuietly(connection);
                return null;
            }

            return connection;
        } catch (IOException e) {
            LOG.warn("建立SSE连接失败: " + e.getMessage(), e);
            closeQuietly(connection);
            return null;
        }
    }

    private void closeQuietly(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                LOG.debug("关闭连接时发生异常", e);
            }
        }
    }

    private void processEventStream(HttpURLConnection connection) throws IOException {
        try (InputStream inputStream = connection.getInputStream();
             InputStreamReader inputReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputReader)) {

            String line;
            while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted() && shouldBeActive.get()) {
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
        }
    }

    private void processEvent(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }

        try {
            Map<String, String> eventMap = JsonUtils.fromJson(message, Map.class);
            if (eventMap != null) {
                String eventType = eventMap.get("event");
                EventProcessor processor = eventProcessors.get(eventType);

                if (processor != null) {
                    processor.process(project, eventMap);
                } else {
                    LOG.debug("收到未知类型事件: " + eventType);
                }
            }
        } catch (Throwable e) {
            LOG.warn("处理SSE事件时发生异常: " + e.getMessage(), e);
        }
    }

    private void handlePongEvent() {
        lastMessageTime = System.currentTimeMillis();
        LOG.debug("收到Pong响应，更新心跳时间戳: " + lastMessageTime);
    }

    private void handleClientConnectedEvent(Project project, Map<String, String> eventMap) {
        try {
            connectionLock.lock();
            clientId = String.valueOf(eventMap.get("clientId"));
            LOG.info("添加SSE客户端: " + clientId + " 项目路径: " + project.getBasePath());

            startHeartbeat();
            startHeartbeatMonitor();

            if (project.isDisposed()) {
                LOG.warn("项目已销毁，跳过会话加载");
                return;
            }

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
            connectionLock.unlock();
        }
    }

    private void handleConnectionError(ConnectionErrorType errorType) {
        int count = retryCount.incrementAndGet();
        transitionState(ConnectionState.CONNECTING, ConnectionState.FAILED);

        String errorMessage = getErrorMessage(errorType);
        LOG.warn("SSE连接失败(" + errorMessage + ")，重试次数: " + count + ", 当前端口: " + currentPort);

        // 根据错误类型采取不同策略
        switch (errorType) {
            case NETWORK_ERROR:
                exponentialBackoffRetry();
                break;
            case AGENT_NOT_RUNNING:
            default:
                // 默认重试策略
                simpleRetry();
        }
    }

    private String getErrorMessage(ConnectionErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "网络连接错误";
            case AGENT_NOT_RUNNING:
                return "Agent未运行";
            case INVALID_PORT:
                return "无效端口";
            default:
                return "未知错误";
        }
    }

    private void exponentialBackoffRetry() {
        try {
            Thread.sleep(currentRetryInterval);
            currentRetryInterval = Math.min(currentRetryInterval * 2, config.maxRetryInterval);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void simpleRetry() {
        try {
            Thread.sleep(config.initialRetryInterval);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void resetConnectionState() {
        try {
            connectionLock.lock();
            transitionState(state.get(), ConnectionState.DISCONNECTED);
            clientId = StringUtils.EMPTY;
        } finally {
            connectionLock.unlock();
        }
    }

    public void disconnect() {
        shouldBeActive.set(false);

        // 取消所有连接任务
        for (Future<?> future : connectionFutures) {
            future.cancel(true);
        }
        connectionFutures.clear();

        removeClientFromServer();

        try {
            connectionLock.lock();
            LOG.info("断开SSE连接: " + clientId + " 项目路径: " + (project != null ? project.getBasePath() : "未知"));
            transitionState(state.get(), ConnectionState.DISCONNECTED);
            clientId = StringUtils.EMPTY;
            retryCount.set(0);
        } finally {
            connectionLock.unlock();
        }
    }

    public void shutdown() {
        disconnect();

        heartbeatScheduler.shutdownNow();
    }

    private void reconnect() {
        CompletableFuture.runAsync(() -> {
            LOG.info("正在重新连接SSE服务...");
            disconnect();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            connect();
        }, threadPool);
    }

    private void startHeartbeatMonitor() {
        try {
            heartbeatLock.lock();
            if (!heartbeatScheduler.isShutdown()) {
                heartbeatScheduler.scheduleAtFixedRate(
                        this::monitorHeartbeat,
                        config.heartbeatMonitorInterval,
                        config.heartbeatMonitorInterval,
                        TimeUnit.MILLISECONDS
                );
                LOG.info("心跳监控已启动");
            }
        } finally {
            heartbeatLock.unlock();
        }
    }

    private void monitorHeartbeat() {
        if (!isConnected() || !shouldBeActive.get()) {
            return;
        }

        long timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime;
        if (timeSinceLastMessage > config.heartbeatInterval * 2) {
            LOG.warn("检测到心跳超时，最后消息时间: " + lastMessageTime +
                    ", 当前时间: " + System.currentTimeMillis() +
                    ", 差值: " + timeSinceLastMessage + "ms");

            if (sendPingMessage()) {
                lastMessageTime = System.currentTimeMillis();
                LOG.info("心跳恢复成功");
            } else {
                LOG.warn("心跳恢复失败，尝试重新连接");
                reconnect();
            }
        }
    }

    private void removeClientFromServer() {
        String currentClientId;
        try {
            connectionLock.lock();
            currentClientId = clientId;
        } finally {
            connectionLock.unlock();
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
        return isConnected() ? clientId : StringUtils.EMPTY;
    }

    private void startHeartbeat() {
        try {
            heartbeatLock.lock();
            if (!heartbeatScheduler.isShutdown()) {
                heartbeatScheduler.scheduleAtFixedRate(
                        this::sendHeartbeat,
                        config.heartbeatInterval,
                        config.heartbeatInterval,
                        TimeUnit.MILLISECONDS
                );
                LOG.info("心跳发送器已启动");
            }
        } finally {
            heartbeatLock.unlock();
        }
    }

    private void sendHeartbeat() {
        if (!isConnected() || !shouldBeActive.get()) {
            return;
        }

        String projectIdentifier = getProjectIdentifier(project);
        LOG.debug("项目[" + projectIdentifier + "] 发送ping消息...");

        if (sendPingMessage()) {
            lastMessageTime = System.currentTimeMillis();
            LOG.debug("项目[" + projectIdentifier + "] ping消息发送成功");
        } else {
            LOG.warn("项目[" + projectIdentifier + "] ping消息发送失败，重新连接...");
            reconnect();
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
                    LOG.info("收到ping响应: " + responseBodyString + ", 请求成功: " + response.isSuccessful());
                }
                return response.isSuccessful();
            }
        } catch (Throwable e) {
            LOG.warn("发送ping消息异常: " + e.getMessage(), e);
            return false;
        }
    }

    private boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    private boolean isConnecting() {
        return state.get() == ConnectionState.CONNECTING || state.get() == ConnectionState.RECONNECTING;
    }

    private boolean transitionState(ConnectionState from, ConnectionState to) {
        boolean success = state.compareAndSet(from, to);
        if (success) {
            LOG.info("SSE连接状态变更: " + from + " -> " + to +
                    ", 项目: " + getProjectIdentifier(project) +
                    ", 端口: " + currentPort);
        }
        return success;
    }

}