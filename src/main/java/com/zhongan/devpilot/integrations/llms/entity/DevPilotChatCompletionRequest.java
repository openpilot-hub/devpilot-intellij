package com.zhongan.devpilot.integrations.llms.entity;

import java.util.ArrayList;
import java.util.List;

public class DevPilotChatCompletionRequest {

    String version = "V240801";

    String encoding = null;

    boolean stream = true;

    List<DevPilotMessage> messages = new ArrayList<>();

    public boolean getStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public List<DevPilotMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<DevPilotMessage> messages) {
        this.messages = messages;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
