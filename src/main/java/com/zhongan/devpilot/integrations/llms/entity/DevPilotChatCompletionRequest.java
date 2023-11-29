package com.zhongan.devpilot.integrations.llms.entity;

import java.util.ArrayList;
import java.util.List;

public class DevPilotChatCompletionRequest {

    String model;

    List<DevPilotMessage> messages = new ArrayList<>();

    boolean stream = Boolean.FALSE;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<DevPilotMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<DevPilotMessage> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

}
