package com.zhongan.devpilot.integrations.llms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DevPilotInstructCompletionRequest {

//    String model;

//    List<DevPilotMessage> messages = new ArrayList<>();

    boolean stream = Boolean.FALSE;

    double temperature = 0L;

    @JsonProperty("max_tokens")
    int maxTokens = 2000;

    int n = 1;

    String prompt = "";

    String suffix = "";

//    public String getModel() {
//        return model;
//    }
//
//    public void setModel(String model) {
//        this.model = model;
//    }

/*
    public List<DevPilotMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<DevPilotMessage> messages) {
        this.messages = messages;
    }
*/

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
