package com.zhongan.devpilot.completions.common.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.zhongan.devpilot.completions.common.prediction.DevPilotCompletion
import java.util.stream.Collectors

class InlineCompletionCache {

    companion object {
        @JvmStatic
        val instance = InlineCompletionCache()

        private val INLINE_COMPLETIONS_LAST_RESULT = Key.create<List<DevPilotCompletion>>("INLINE_COMPLETIONS_LAST_RESULT")
    }

    fun store(editor: Editor, completions: List<DevPilotCompletion>) {
        editor.putUserData(INLINE_COMPLETIONS_LAST_RESULT, completions)
    }

    fun retrieveAdjustedCompletions(editor: Editor, userInput: String): List<DevPilotCompletion> {
        val completions = editor.getUserData(INLINE_COMPLETIONS_LAST_RESULT)
            ?: return emptyList()
        return completions.stream()
            .filter { completion: DevPilotCompletion -> completion.suffix.startsWith(userInput) }
            .map { completion: DevPilotCompletion ->
                completion.createAdjustedCompletion(
                    completion.oldPrefix + userInput,
                    completion.cursorPrefix + userInput
                )
            }
            .collect(Collectors.toList())
    }

    fun clear(editor: Editor) {
        editor.putUserData(INLINE_COMPLETIONS_LAST_RESULT, null)
    }
}
