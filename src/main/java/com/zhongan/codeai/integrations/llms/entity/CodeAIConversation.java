package com.zhongan.codeai.integrations.llms.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CodeAIConversation {

    private UUID id;

    private String model;

    private List<CodeAIMessage> messages = new ArrayList<>();

    private LocalDateTime createTime;

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
