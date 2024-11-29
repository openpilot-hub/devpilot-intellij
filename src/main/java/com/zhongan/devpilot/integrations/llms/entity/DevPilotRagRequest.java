package com.zhongan.devpilot.integrations.llms.entity;

public class DevPilotRagRequest {
    private String projectType;

    private String content;

    private String selectedCode;

    private String projectName;

    private String predictionComments;

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSelectedCode() {
        return selectedCode;
    }

    public void setSelectedCode(String selectedCode) {
        this.selectedCode = selectedCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPredictionComments() {
        return predictionComments;
    }

    public void setPredictionComments(String predictionComments) {
        this.predictionComments = predictionComments;
    }
}
