package com.zhongan.devpilot.completions.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.zhongan.devpilot.completions.general.SuggestionTrigger
import com.zhongan.devpilot.completions.inline.DebounceUtils.getDebounceInterval

object CompletionTracker {
    private val LAST_COMPLETION_REQUEST_TIME = Key.create<Long>("LAST_COMPLETION_REQUEST_TIME")
    private val DEBOUNCE_INTERVAL_MS = getDebounceInterval()

    @JvmStatic
    fun calcDebounceTimeMs(editor: Editor, completionAdjustment: CompletionAdjustment): Long {
        if (completionAdjustment.suggestionTrigger == SuggestionTrigger.LookAhead) {
            return 0
        }

        val lastCompletionTimestamp = LAST_COMPLETION_REQUEST_TIME[editor]
        if (lastCompletionTimestamp != null) {
            val elapsedTimeFromLastEvent = System.currentTimeMillis() - lastCompletionTimestamp
            if (elapsedTimeFromLastEvent < DEBOUNCE_INTERVAL_MS) {
                return DEBOUNCE_INTERVAL_MS - elapsedTimeFromLastEvent
            }
        }
        return 0
    }

    @JvmStatic
    fun updateLastCompletionRequestTime(editor: Editor) {
        val currentTimestamp = System.currentTimeMillis()
        LAST_COMPLETION_REQUEST_TIME[editor] = currentTimestamp
    }
}
