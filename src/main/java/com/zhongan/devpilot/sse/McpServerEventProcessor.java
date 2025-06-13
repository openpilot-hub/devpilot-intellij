package com.zhongan.devpilot.sse;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.mcp.McpConfigurationHandler;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class McpServerEventProcessor {

    private static final Logger LOG = Logger.getInstance(McpServerEventProcessor.class);

    public static final McpServerEventProcessor INSTANCE = new McpServerEventProcessor();

    public void processMcpServerEvent(Project project, Map<String, String> eventData) {
        try {
            String tag = eventData.get("tag");
            if (StringUtils.isEmpty(tag)) {
                LOG.warn("Session event missing tag information");
                return;
            }
            if (StringUtils.equalsIgnoreCase("Refreshed", tag)) {
                McpConfigurationHandler.INSTANCE.refreshMcpServers();
            }
        } catch (Exception e) {
            LOG.error("Error processing session event", e);
        }
    }
}
