package com.zhongan.devpilot.integrations.llms.entity;

public class DevPilotChatCompletionResponse {

    private boolean successful;

    private String content;

    public DevPilotChatCompletionResponse(boolean successful, String content) {
        this.successful = successful;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(Boolean successful) {
        this.successful = successful;
    }

    public static DevPilotChatCompletionResponse success(String content) {
        return new DevPilotChatCompletionResponse(Boolean.TRUE, content);
    }

    public static DevPilotChatCompletionResponse failed(String content) {
        return new DevPilotChatCompletionResponse(Boolean.FALSE, content);
    }

}
