package com.zhongan.devpilot.completions.requests;

import com.zhongan.devpilot.completions.general.CompletionKind;

import java.util.Map;

public class CompletionMetadata {
    private String detail;

    private CompletionKind completionKind;

    private Map<String, Object> snippetContext;

    private Boolean isCached;

    private Boolean deprecated;

    public CompletionMetadata(String detail, CompletionKind completionKind, Map<String, Object> snippetContext, Boolean isCached, Boolean deprecated) {
        this.detail = detail;
        this.completionKind = completionKind;
        this.snippetContext = snippetContext;
        this.isCached = isCached;
        this.deprecated = deprecated;
    }

    public String getDetail() {
        return detail;
    }

    public CompletionKind getCompletionKind() {
        return completionKind;
    }

    public Map<String, Object> getSnippetContext() {
        return snippetContext;
    }

    public Boolean getIsCached() {
        return isCached;
    }

    public Boolean getIsDeprecated() {
        return deprecated != null ? deprecated : false;
    }
}