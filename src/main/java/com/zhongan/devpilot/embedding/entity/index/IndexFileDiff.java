package com.zhongan.devpilot.embedding.entity.index;

import java.util.List;

public class IndexFileDiff {
    private String projectName;

    private String gitRepo;

    private List<IndexedFile> modifiedFileList;

    private List<IndexedFile> addedFileList;

    private List<IndexedFile> deletedFileList;

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

    public List<IndexedFile> getModifiedFileList() {
        return modifiedFileList;
    }

    public void setModifiedFileList(List<IndexedFile> modifiedFileList) {
        this.modifiedFileList = modifiedFileList;
    }

    public List<IndexedFile> getAddedFileList() {
        return addedFileList;
    }

    public void setAddedFileList(List<IndexedFile> addedFileList) {
        this.addedFileList = addedFileList;
    }

    public List<IndexedFile> getDeletedFileList() {
        return deletedFileList;
    }

    public void setDeletedFileList(List<IndexedFile> deletedFileList) {
        this.deletedFileList = deletedFileList;
    }
}
