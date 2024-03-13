//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
        editor.putUserData(INLINE_COMPLETIONS_LAST_RESULT, (List<DevPilotCompletion>) null);
    }

    public List<DevPilotCompletion> retrieveAdjustedCompletions(Editor editor, String userInput) {
        List<DevPilotCompletion> completions = (List)editor.getUserData(INLINE_COMPLETIONS_LAST_RESULT);
        return completions == null ? Collections.emptyList() : (List)completions.stream().filter((completion) -> {
            return completion.getSuffix().startsWith(userInput);
        }).map((completion) -> {
            String var10001 = completion.getOldPrefix() + userInput;
            String var10002 = completion.getCursorPrefix();
            return completion.createAdjustedCompletion(var10001, var10002 + userInput);
        }).collect(Collectors.toList());
    }
}
