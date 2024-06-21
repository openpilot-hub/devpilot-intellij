package com.zhongan.devpilot.util;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;

import java.util.List;
import java.util.Map;

public class MessageUtil {

    public static DevPilotMessage createMessage(String id, String role, String msgType, String content, List<String> additional) {
        DevPilotMessage message = new DevPilotMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        message.setCommandType(msgType);
        message.setAdditional(additional);
        return message;
    }

    public static DevPilotMessage createPromptMessage(String id, String msgType, Map<String, String> data, List<String> additional) {
        DevPilotMessage message = new DevPilotMessage();
        message.setId(id);
        message.setRole("user");
        message.setPromptData(data);
        message.setCommandType(msgType);
        message.setAdditional(additional);
        return message;
    }

    public static DevPilotMessage createUserMessage(String content, String msgType, String id, List<String> additional) {
        return createMessage(id, "user", msgType, content, additional);
    }

    public static DevPilotMessage createSystemMessage(String content) {
        return createMessage("-1", "system", "no-ops", content, null);
    }

}

