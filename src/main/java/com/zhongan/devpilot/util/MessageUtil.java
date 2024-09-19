package com.zhongan.devpilot.util;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;

import java.util.Map;

public class MessageUtil {

    public static DevPilotMessage createMessage(String id, String role, String msgType, String content) {
        DevPilotMessage message = new DevPilotMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        message.setCommandType(msgType);
        return message;
    }

    public static DevPilotMessage createPromptMessage(String id, String msgType, Map<String, String> data) {
        DevPilotMessage message = new DevPilotMessage();
        message.setId(id);
        message.setRole("user");
        message.setPromptData(data);
        message.setCommandType(msgType);
        return message;
    }

    public static DevPilotMessage createPromptMessage(String id, String msgType, String content, Map<String, String> data) {
        DevPilotMessage message = new DevPilotMessage();
        message.setId(id);
        message.setRole("user");
        message.setPromptData(data);
        message.setCommandType(msgType);
        message.setContent(content);
        return message;
    }

    public static DevPilotMessage createUserMessage(String content, String msgType, String id) {
        return createMessage(id, "user", msgType, content);
    }

}

