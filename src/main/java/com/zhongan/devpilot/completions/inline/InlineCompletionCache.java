package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Key;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InlineCompletionCache {
    public static final InlineCompletionCache INSTANCE = new InlineCompletionCache();

    private static final Key<List<DevPilotCompletion>> INLINE_COMPLETIONS_LAST_RESULT = Key.create("INLINE_COMPLETIONS_LAST_RESULT");

    private InlineCompletionCache() {
    }

    public static void store(Editor editor, List<DevPilotCompletion> completions) {
        editor.putUserData(INLINE_COMPLETIONS_LAST_RESULT, completions);
    }

    public static void clear(Editor editor) {
        editor.putUserData(INLINE_COMPLETIONS_LAST_RESULT, null);
    }

    public List<DevPilotCompletion> retrieveAdjustedCompletions(Editor editor, String userInput) {
        List<DevPilotCompletion> completions = editor.getUserData(INLINE_COMPLETIONS_LAST_RESULT);
        if (completions == null) {
            return Collections.emptyList();
        }
        return completions.stream()
            .filter(completion -> completion.getSuffix().startsWith(userInput))
            .map(completion -> completion.createAdjustedCompletion(
                completion.getOldPrefix() + userInput,
                completion.getCursorPrefix() + userInput))
            .collect(Collectors.toList());
    }
}
