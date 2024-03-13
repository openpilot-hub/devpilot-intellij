//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.general.Utils;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.completions.requests.ResultEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompletionUtils {
    public CompletionUtils() {
    }

    private static @Nullable String getCursorPrefix(@NotNull Document document, int cursorPosition) {


        try {
            int lineNumber = document.getLineNumber(cursorPosition);
            int lineStart = document.getLineStartOffset(lineNumber);
            return document.getText(TextRange.create(lineStart, cursorPosition)).trim();
        } catch (Throwable var4) {
            Logger.getInstance(CompletionUtils.class).warn("Failed to get cursor prefix: ", var4);
            return null;
        }
    }

    private static @Nullable String getCursorSuffix(@NotNull Document document, int cursorPosition) {


        try {
            int lineNumber = document.getLineNumber(cursorPosition);
            int lineEnd = document.getLineEndOffset(lineNumber);
            return document.getText(TextRange.create(cursorPosition, lineEnd)).trim();
        } catch (Throwable var4) {
            Logger.getInstance(CompletionUtils.class).warn("Failed to get cursor suffix: ", var4);
            return null;
        }
    }

    public static @Nullable DevPilotCompletion createDevpilotCompletion(@NotNull Document document, int offset, String oldPrefix, ResultEntry result, int index, SuggestionTrigger suggestionTrigger) {


        String cursorPrefix = getCursorPrefix(document, offset);
        String cursorSuffix = getCursorSuffix(document, offset);
        return cursorPrefix != null && cursorSuffix != null ? new DevPilotCompletion(oldPrefix, result.newPrefix, result.oldSuffix, result.newSuffix, index, cursorPrefix, cursorSuffix, result.completionMetadata, suggestionTrigger) : null;
    }

    public static int completionLimit(CompletionParameters parameters, CompletionResultSet result, boolean isLocked) {
        return completionLimit(parameters.getEditor().getDocument(), result.getPrefixMatcher().getPrefix(), parameters.getOffset(), isLocked);
    }

    public static int completionLimit(@NotNull Document document, @NotNull String prefix, int offset, boolean isLocked) {

        if (isLocked) {
            return 1;
        } else {
            boolean preferDevPilot = !Utils.endsWithADot(document, offset - prefix.length());
            return preferDevPilot ? 5 : 1;
        }
    }
}
