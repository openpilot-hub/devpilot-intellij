package com.zhongan.devpilot.webview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddedModel {

    private Boolean embedded;

    private String repoName;

    public EmbeddedModel(Boolean embedded, String repoName) {
        this.embedded = embedded;
        this.repoName = repoName;
    }

    public Boolean getEmbedded() {
        return embedded;
    }

    public void setEmbedded(Boolean embedded) {
        this.embedded = embedded;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }
}
