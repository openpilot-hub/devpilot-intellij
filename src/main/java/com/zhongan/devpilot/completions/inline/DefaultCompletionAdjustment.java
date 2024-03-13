//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.general.SuggestionTrigger;

public class DefaultCompletionAdjustment extends CompletionAdjustment {
    public DefaultCompletionAdjustment() {
    }

    public SuggestionTrigger getSuggestionTrigger() {
        return SuggestionTrigger.DocumentChanged;
    }
}
