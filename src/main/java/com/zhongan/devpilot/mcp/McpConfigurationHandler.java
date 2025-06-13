package com.zhongan.devpilot.mcp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.MapUtils;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.zhongan.devpilot.constant.DefaultConst.MCP_CONNECTIONS_PATH;
import static com.zhongan.devpilot.constant.DefaultConst.REMOTE_AGENT_DEFAULT_HOST;
import static com.zhongan.devpilot.constant.DefaultConst.UPDATE_MCP_SERVER_PATH;
import static com.zhongan.devpilot.util.EventUtil.sendEventRequest;

public class McpConfigurationHandler {
    private static final Logger LOG = Logger.getInstance(McpConfigurationHandler.class);

    public static final McpConfigurationHandler INSTANCE = new McpConfigurationHandler();

    private static McpConnections mcpConnections = null;

    public void handleConfigFileChanged(String path, long lastModified) {
        Map<String, Object> params = new HashMap<>();
        params.put("path", path);
        params.put("lastModified", lastModified);
        publishEvent("McpServers-Changed", JsonUtils.toJson(params));
    }

    public McpConnections handleMcpServerChanged(String operatorType, McpServer server) {
        mcpConnections = null;
        Map<String, Object> params = new HashMap<>();
        params.put("path", mcpConfigurationPath());
        params.put("operatorType", operatorType);
        params.put("server", server);
        return updateMcpServer(params);
//        publishEvent("McpServer-Changed", JsonUtils.toJson(params));
    }

    public void refreshMcpServers() {
        mcpConnections = loadMcpConnections();
        LOG.warn("Refresh " + mcpConnections.getMcpServers().size() + " mcp servers.");
    }

    public McpConnections loadMcpServersWithConnectionStatus(boolean forceUpdate) {
        if (forceUpdate || null == mcpConnections || MapUtils.isEmpty(mcpConnections.getMcpServers())) {
            mcpConnections = loadMcpConnections();
        }
        return mcpConnections;
    }

    public void initialMcpServer() {
        String path = mcpConfigurationPath();
        ensureFileExists(path);

        Map<String, Object> params = new HashMap<>();
        params.put("path", path);
        try {
            params.put("lastModified", Files.getLastModifiedTime(Paths.get(path)).to(TimeUnit.MILLISECONDS));
        } catch (IOException e) {
            LOG.warn("Exception occurred while getting last modified time of mcp_configuration.json file.");
            params.put("lastModified", System.currentTimeMillis());
        }

        publishEvent("McpServer-Initial", JsonUtils.toJson(params));
    }

    public synchronized void ensureFileExists(String path) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Map<String, Map<String, McpServer>> mcpSettings = new HashMap<>();
                mcpSettings.put("mcpServers", new HashMap<>());
                String jsonContent = JsonUtils.toJson(mcpSettings);
                Files.write(filePath, jsonContent.getBytes());
                LOG.info("文件不存在，已创建新文件：" + path);
            }
        } catch (IOException e) {
            LOG.error("创建文件时发生错误：" + path, e);
        }
    }

    public String mcpConfigurationPath() {
        return BinaryManager.INSTANCE.getHomeDir() + "/mcp_configuration.json";
    }

    private void publishEvent(String event, String data) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            LOG.info("mcp_configuration.json file has been changed.");
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("eventType", event);
                body.put("data", data);
                sendEventRequest(body);
            } catch (Exception e) {
                LOG.warn("Exception occurred while handling configuration file changed.", e);
            }
        });
    }

    public McpConnections loadMcpConnections() {
        try {
            Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();
            if (null != portPId) {
                String url = REMOTE_AGENT_DEFAULT_HOST + portPId.first + MCP_CONNECTIONS_PATH;
                var request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", UserAgentUtils.buildUserAgent())
                        .header("Auth-Type", LoginUtils.getLoginType())
                        .get()
                        .build();

                Call call = OkhttpUtils.getClient().newCall(request);
                try (Response response = call.execute()) {
                    LOG.info("Load MCP connections response: " + response);

                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return JsonUtils.fromJson(responseBody, McpConnections.class);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Exception occurred while sending event request.", e);
        }
        return new McpConnections();
    }

    public McpConnections updateMcpServer(Map<String, Object> params) {
        try {
            Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();
            if (null != portPId) {
                String url = REMOTE_AGENT_DEFAULT_HOST + portPId.first + UPDATE_MCP_SERVER_PATH;

                var request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", UserAgentUtils.buildUserAgent())
                        .header("Auth-Type", LoginUtils.getLoginType())
                        .put(RequestBody.create(JsonUtils.toJson(params), MediaType.parse("application/json")))
                        .build();

                Call call = OkhttpUtils.getClient().newCall(request);
                try (Response response = call.execute()) {
                    LOG.info("Update MCP and reload connections response: " + response);

                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return JsonUtils.fromJson(responseBody, McpConnections.class);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Exception occurred while sending event request.", e);
        }
        return new McpConnections();
    }

}
