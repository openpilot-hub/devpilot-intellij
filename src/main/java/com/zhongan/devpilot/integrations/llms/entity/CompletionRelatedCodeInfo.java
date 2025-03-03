package com.zhongan.devpilot.integrations.llms.entity;

public class CompletionRelatedCodeInfo {
    private double score;

    private String filePath;

    private String code;

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
