package com.zhongan.codeai.integrations.llms.entity;

import java.util.ArrayList;
import java.util.List;

public class CodeAIChatCompletionRequest {

    String model;

    List<CodeAIMessage> messages = new ArrayList<>();

    boolean stream = Boolean.FALSE;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<CodeAIMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<CodeAIMessage> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

}
