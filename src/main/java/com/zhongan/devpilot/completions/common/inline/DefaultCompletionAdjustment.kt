package com.zhongan.devpilot.completions.common.inline

import com.zhongan.devpilot.completions.common.general.SuggestionTrigger

class DefaultCompletionAdjustment : CompletionAdjustment() {

    override val suggestionTrigger: SuggestionTrigger
        get() = SuggestionTrigger.DocumentChanged
}
