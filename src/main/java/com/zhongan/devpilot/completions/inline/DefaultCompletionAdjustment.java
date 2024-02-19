package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.general.SuggestionTrigger;

public class DefaultCompletionAdjustment extends CompletionAdjustment {

    @Override
    public SuggestionTrigger getSuggestionTrigger() {
        return SuggestionTrigger.DocumentChanged;
    }
}
