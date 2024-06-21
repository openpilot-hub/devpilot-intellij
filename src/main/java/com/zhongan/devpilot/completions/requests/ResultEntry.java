package com.zhongan.devpilot.completions.requests;

import com.zhongan.devpilot.completions.Completion;
import com.zhongan.devpilot.completions.general.CompletionKind;

public class ResultEntry implements Completion {
    public String id;

    public String newPrefix;

    public String oldSuffix;

    public String newSuffix;

    public CompletionMetadata completionMetadata;

    @Override
    public boolean isSnippet() {
        if (this.completionMetadata == null) {
            return false;
        }

        return this.completionMetadata.getCompletionKind() == CompletionKind.Snippet;
    }
}
