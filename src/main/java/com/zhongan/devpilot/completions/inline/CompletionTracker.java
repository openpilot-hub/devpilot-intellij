//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Key;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;

public class CompletionTracker {
    private static final Key<Long> LAST_COMPLETION_REQUEST_TIME = Key.create("LAST_COMPLETION_REQUEST_TIME");
    private static final long DEBOUNCE_INTERVAL_MS = DebounceUtils.getDebounceInterval();

    public CompletionTracker() {
    }

    public static long calcDebounceTimeMs(Editor editor, CompletionAdjustment completionAdjustment) {
        if (completionAdjustment.getSuggestionTrigger() == SuggestionTrigger.LookAhead) {
            return 0L;
        } else {
            Long lastCompletionTimestamp = (Long)LAST_COMPLETION_REQUEST_TIME.get(editor);
            if (lastCompletionTimestamp != null) {
                long elapsedTimeFromLastEvent = System.currentTimeMillis() - lastCompletionTimestamp;
                if (elapsedTimeFromLastEvent < DEBOUNCE_INTERVAL_MS) {
                    return DEBOUNCE_INTERVAL_MS - elapsedTimeFromLastEvent;
                }
            }

            return 0L;
        }
    }

    public static void updateLastCompletionRequestTime(Editor editor) {
        long currentTimestamp = System.currentTimeMillis();
        LAST_COMPLETION_REQUEST_TIME.set(editor, currentTimestamp);
    }
}
