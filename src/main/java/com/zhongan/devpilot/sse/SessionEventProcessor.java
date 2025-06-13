package com.zhongan.devpilot.sse;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.session.ChatSessionManagerService;
import com.zhongan.devpilot.session.model.ChatSession;
import com.zhongan.devpilot.util.JsonUtils;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class SessionEventProcessor {

    private static final Logger LOG = Logger.getInstance(SessionEventProcessor.class);

    public static final SessionEventProcessor INSTANCE = new SessionEventProcessor();

    public void processSessionEvent(Project project, Map<String, String> eventData) {
        try {
            String tag = eventData.get("tag");
            if (StringUtils.isEmpty(tag)) {
                LOG.warn("Session event missing tag information");
                return;
            }
            switch (tag) {
                case "Updated":
                    project.getService(ChatSessionManagerService.class).getSessionManager().setSessionUpdated(Boolean.TRUE);
                    break;
                case "Request-Saved":
                    ChatSession eventSession = JsonUtils.fromJson(eventData.get("message"), ChatSession.class);
                    project.getService(ChatSessionManagerService.class).getSessionManager().handleRequestMessageListSaved(eventSession);
                    break;
                default:
                    LOG.warn("Unknown session event tag: " + tag);
                    break;
            }
        } catch (Exception e) {
            LOG.error("Error processing session event", e);
        }
    }

}