package com.zhongan.devpilot.integrations.llms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.editor.Editor;

import java.util.List;

public class DevPilotInstructCompletionRequest {

    private Editor editor;

    private int offset;

    boolean stream = Boolean.FALSE;

    double temperature = 0L;

    @JsonProperty("max_tokens")
    int maxTokens = 2000;

    int n = 1;

    String prompt = "";

    String suffix = "";

    String completionType = "inline";

    String encoding = null;

    List<CompletionRelatedCodeInfo> relatedCodeInfos;

    public Editor getEditor() {
        return editor;
    }

    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

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

    public String getCompletionType() {
        return completionType;
    }

    public void setCompletionType(String completionType) {
        this.completionType = completionType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public List<CompletionRelatedCodeInfo> getRelatedCodeInfos() {
        return relatedCodeInfos;
    }

    public void setRelatedCodeInfos(List<CompletionRelatedCodeInfo> relatedCodeInfos) {
        this.relatedCodeInfos = relatedCodeInfos;
    }
}
