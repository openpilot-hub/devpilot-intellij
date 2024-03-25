package com.zhongan.devpilot.webview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddedModel {

    private Boolean repoEmbedded;

    private String repoName;

    public EmbeddedModel(Boolean embedded, String repoName) {
        this.repoEmbedded = embedded;
        this.repoName = repoName;
    }

    public Boolean getRepoEmbedded() {
        return repoEmbedded;
    }

    public void setRepoEmbedded(Boolean repoEmbedded) {
        this.repoEmbedded = repoEmbedded;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }
}
