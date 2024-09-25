package com.zhongan.devpilot.integrations.llms.entity;

import java.util.ArrayList;
import java.util.List;

public class DevPilotCodePrediction {
    private List<String> inputArgs = new ArrayList<>(2);

    private List<String> outputArgs = new ArrayList<>(2);

    private List<String> references = new ArrayList<>();

    private String comments;

    public List<String> getInputArgs() {
        return inputArgs;
    }

    public void setInputArgs(List<String> inputArgs) {
        this.inputArgs = inputArgs;
    }

    public List<String> getOutputArgs() {
        return outputArgs;
    }

    public void setOutputArgs(List<String> outputArgs) {
        this.outputArgs = outputArgs;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
