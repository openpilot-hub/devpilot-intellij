package com.zhongan.devpilot.util;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;

public class MessageUtil {

    public static DevPilotMessage createMessage(String role, String content) {
        DevPilotMessage message = new DevPilotMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    public static DevPilotMessage createUserMessage(String content) {
        return createMessage("user", content);
    }

    public static DevPilotMessage createSystemMessage(String content) {
        return createMessage("system", content);
    }

}

