//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
        return this.detail;
    }

    public CompletionKind getCompletionKind() {
        return this.completionKind;
    }

    public Map<String, Object> getSnippetContext() {
        return this.snippetContext;
    }

    public Boolean getIsCached() {
        return this.isCached;
    }

    public Boolean getIsDeprecated() {
        return this.deprecated != null ? this.deprecated : false;
    }
}
