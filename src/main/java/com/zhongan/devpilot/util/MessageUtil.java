package com.zhongan.devpilot.util;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;

public class MessageUtil {

    public static DevPilotMessage createMessage(String id, String role, String content) {
        DevPilotMessage message = new DevPilotMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    public static DevPilotMessage createUserMessage(String content, String id) {
        return createMessage(id, "user", content);
    }

    public static DevPilotMessage createSystemMessage(String content) {
        return createMessage("-1", "system", content);
    }

}

