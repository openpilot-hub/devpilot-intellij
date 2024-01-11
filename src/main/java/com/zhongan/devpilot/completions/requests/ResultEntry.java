package com.zhongan.devpilot.completions.requests;

import com.zhongan.devpilot.completions.general.CompletionKind;
import com.zhongan.devpilot.completions.Completion;

public class ResultEntry implements Completion {
    public String new_prefix;
    public String old_suffix;
    public String new_suffix;
    public CompletionMetadata completionMetadata;

    @Override
    public boolean isSnippet() {
        if (this.completionMetadata == null) {
            return false;
        }

        return this.completionMetadata.getCompletionKind() == CompletionKind.Snippet;
    }
}
