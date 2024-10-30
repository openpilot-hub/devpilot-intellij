package com.zhongan.devpilot.completions;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.completions.requests.ResultEntry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.zhongan.devpilot.completions.general.StaticConfig.MAX_COMPLETIONS;
import static com.zhongan.devpilot.completions.general.Utils.endsWithADot;

public class CompletionUtils {

    @Nullable
    private static String getCursorPrefix(@NotNull Document document, int cursorPosition) {
        try {
            int lineNumber = document.getLineNumber(cursorPosition);
            int lineStart = document.getLineStartOffset(lineNumber);

            return document.getText(TextRange.create(lineStart, cursorPosition)).trim();
        } catch (Throwable e) {
            Logger.getInstance(CompletionUtils.class).warn("Failed to get cursor prefix: ", e);
            return null;
        }
    }

    @Nullable
    private static String getCursorSuffix(@NotNull Document document, int cursorPosition) {
        try {
            int lineNumber = document.getLineNumber(cursorPosition);
            int lineEnd = document.getLineEndOffset(lineNumber);

            return document.getText(TextRange.create(cursorPosition, lineEnd)).trim();
        } catch (Throwable e) {
            Logger.getInstance(CompletionUtils.class).warn("Failed to get cursor suffix: ", e);
            return null;
        }
    }

    @Nullable
    public static DevPilotCompletion createDevpilotCompletion(
            Editor editor,
            @NotNull Document document,
            int offset,
            String oldPrefix,
            ResultEntry result,
            int index,
            SuggestionTrigger suggestionTrigger) {
        String cursorPrefix = CompletionUtils.getCursorPrefix(document, offset);
        String cursorSuffix = CompletionUtils.getCursorSuffix(document, offset);
        if (cursorPrefix == null || cursorSuffix == null) {
            return null;
        }

        return new DevPilotCompletion(
                editor,
                result.id,
                oldPrefix,
                result.newPrefix,
                result.oldSuffix,
                result.newSuffix,
                index,
                cursorPrefix,
                cursorSuffix,
                result.completionMetadata,
                suggestionTrigger);
    }

    @Nullable
    public static DevPilotCompletion createSimpleDevpilotCompletion(
            Editor editor,
            int offset,
            String oldPrefix,
            String newPrefix,
            String id,
            @NotNull Document document) {
        String cursorPrefix = CompletionUtils.getCursorPrefix(document, offset);
        String cursorSuffix = CompletionUtils.getCursorSuffix(document, offset);
        if (cursorPrefix == null || cursorSuffix == null) {
            return null;
        }
        return new DevPilotCompletion(
                editor,
                id,
                oldPrefix,
                newPrefix,
                "",
                "",
                0,
                cursorPrefix,
                cursorSuffix,
                null,
                null);
    }

    public static int completionLimit(
            CompletionParameters parameters, CompletionResultSet result, boolean isLocked) {
        return completionLimit(
                parameters.getEditor().getDocument(),
                result.getPrefixMatcher().getPrefix(),
                parameters.getOffset(),
                isLocked);
    }

    public static int completionLimit(
            @NotNull Document document, @NotNull String prefix, int offset, boolean isLocked) {
        if (isLocked) {
            return 1;
        }
        boolean preferDevPilot = !endsWithADot(document, offset - prefix.length());

        return preferDevPilot ? MAX_COMPLETIONS : 1;
    }
}
