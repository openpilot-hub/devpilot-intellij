package com.zhongan.devpilot.common.inline

import com.zhongan.devpilot.common.general.SuggestionTrigger

class DefaultCompletionAdjustment : CompletionAdjustment() {

    override val suggestionTrigger: SuggestionTrigger
        get() = SuggestionTrigger.DocumentChanged
}
