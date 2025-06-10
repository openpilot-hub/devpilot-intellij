package com.zhongan.devpilot.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class McpConnections {

    private Map<String, ServerStatus> connections;

    private Map<String, McpServer> mcpServers;

    public static class ServerStatus {
        private String status;

        private String error;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public Map<String, ServerStatus> getConnections() {
        return connections;
    }

    public void setConnections(Map<String, ServerStatus> connections) {
        this.connections = connections;
    }

    public Map<String, McpServer> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(Map<String, McpServer> mcpServers) {
        this.mcpServers = mcpServers;
    }
}
