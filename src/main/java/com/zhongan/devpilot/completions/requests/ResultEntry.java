//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.requests;

import com.zhongan.devpilot.completions.Completion;
import com.zhongan.devpilot.completions.general.CompletionKind;

public class ResultEntry implements Completion {
    public String newPrefix;
    public String oldSuffix;
    public String newSuffix;
    public CompletionMetadata completionMetadata;

    public ResultEntry() {
    }

    public boolean isSnippet() {
        if (this.completionMetadata == null) {
            return false;
        } else {
            return this.completionMetadata.getCompletionKind() == CompletionKind.Snippet;
        }
    }
}
