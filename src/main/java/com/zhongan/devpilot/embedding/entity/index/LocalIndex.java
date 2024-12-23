package com.zhongan.devpilot.embedding.entity.index;

import java.util.List;

public class LocalIndex {
    private String version = "DevPilot-1.0.0";

    private String projectName;

    private String gitRepo;

    private List<IndexedFile> indexedFiles;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public List<IndexedFile> getIndexedFiles() {
        return indexedFiles;
    }

    public void setIndexedFiles(List<IndexedFile> indexedFiles) {
        this.indexedFiles = indexedFiles;
    }
}
