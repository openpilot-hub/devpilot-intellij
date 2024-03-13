//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.integrations.llms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DevPilotInstructCompletionRequest {
    boolean stream;
    double temperature;
    @JsonProperty("max_tokens")
    int maxTokens;
    int n;
    String prompt;
    String suffix;

    String model;
    List<DevPilotMessage> messages;

    public List<DevPilotMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<DevPilotMessage> messages) {
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public DevPilotInstructCompletionRequest() {
        this.stream = Boolean.FALSE;
        this.temperature = 0.0;
        this.maxTokens = 2000;
        this.n = 1;
        this.prompt = "";
        this.suffix = "";
        this.messages = new ArrayList<>();
    }

    public boolean isStream() {
        return this.stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public double getTemperature() {
        return this.temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return this.maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getN() {
        return this.n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getPrompt() {
        return this.prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
