package com.zhongan.devpilot.embedding.entity.request;

import java.util.List;
import java.util.Map;

public class EmbeddingChunkRequest {
    private String batchId;

    private String homeDir;

    private boolean submitEnd;

    private String projectName;

    private String gitRepo;

    private Map<String, List<VectorIndexRequest>> addedRecords;

    private Map<String, List<VectorIndexRequest>> changedRecords;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(String homeDir) {
        this.homeDir = homeDir;
    }

    public boolean isSubmitEnd() {
        return submitEnd;
    }

    public void setSubmitEnd(boolean submitEnd) {
        this.submitEnd = submitEnd;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getGitRepo() {
        return gitRepo;
    }

    public void setGitRepo(String gitRepo) {
        this.gitRepo = gitRepo;
    }

    public Map<String, List<VectorIndexRequest>> getAddedRecords() {
        return addedRecords;
    }

    public void setAddedRecords(Map<String, List<VectorIndexRequest>> addedRecords) {
        this.addedRecords = addedRecords;
    }

    public Map<String, List<VectorIndexRequest>> getChangedRecords() {
        return changedRecords;
    }

    public void setChangedRecords(Map<String, List<VectorIndexRequest>> changedRecords) {
        this.changedRecords = changedRecords;
    }
}
