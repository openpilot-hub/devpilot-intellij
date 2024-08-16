package com.zhongan.devpilot.completions.inline;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.util.CommentUtil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.zhongan.devpilot.completions.general.DependencyContainer.singletonOfInlineCompletionHandler;

public class CompletionUtils {
    private static final Pattern END_OF_LINE_VALID_PATTERN = Pattern.compile("^\\s*[)}\\]\"'`]*\\s*[:{;,]?\\s*$");

    private static TriggerInfo lastTriggerInfo = new TriggerInfo();

    public static boolean isValidDocumentChange(Document document, int newOffset, int previousOffset) {
        if (newOffset < 0 || previousOffset > newOffset) return false;

        String addedText = document.getText(new TextRange(previousOffset, newOffset));
        return
                isValidMidlinePosition(document, newOffset) &&
                        isValidNonEmptyChange(addedText.length(), addedText) &&
                        isSingleCharNonWhitespaceChange(addedText);
    }

    public static boolean isValidMidlinePosition(Document document, int offset) {
        int lineIndex = document.getLineNumber(offset);
        TextRange lineRange = TextRange.create(document.getLineStartOffset(lineIndex), document.getLineEndOffset(lineIndex));
        String line = document.getText(lineRange);
        String lineSuffix = line.substring(offset - lineRange.getStartOffset());
        return END_OF_LINE_VALID_PATTERN.matcher(lineSuffix).matches();
    }

    public static boolean isValidNonEmptyChange(int replacedTextLength, String newText) {
        return replacedTextLength >= 0 && !newText.isEmpty();
    }

    public static boolean isSingleCharNonWhitespaceChange(String newText) {
        return newText.trim().length() <= 1;
    }

    // Limit trigger condition, avoid too much unnecessary request
    public static VerifyResult ignoreLogicalPositionTrigger(String newText, String currentLineText, Language language) {
        boolean isPreComment = CommentUtil.containsComment(StringUtils.trim(currentLineText), language);

        // code end with "{"
        boolean endWithLBrace = StringUtils.endsWith(StringUtils.trim(currentLineText), "{");

        // only contains empty and tab
        boolean emptyAndTabChar = StringUtils.isEmpty(StringUtils.trim(newText));
        boolean currentLineEmpty = StringUtils.isEmpty(StringUtils.trim(currentLineText));
        if (emptyAndTabChar && currentLineEmpty) {
            return VerifyResult.create(true);
        }

        boolean newlineChar = StringUtils.startsWith(newText, "\n");

        if (newlineChar) {
            if (endWithLBrace || isPreComment) {
                return VerifyResult.createComment(false);
            } else {
                return VerifyResult.create(true);
            }
        }

        return VerifyResult.create(false);
    }

    public static VerifyResult ignoreFromCurrentLineChange(String newText, String currentLineText, Language language) {
        if (StringUtils.endsWith(StringUtils.trim(newText), ";")) {
            return VerifyResult.create(true);
        }

        // code end with ";"
        boolean endsWithSemicolon = StringUtils.endsWith(StringUtils.trim(currentLineText), ";");

        // code end with "{"
        boolean endWithLBrace = StringUtils.endsWith(StringUtils.trim(currentLineText), "{");

        // code end with "}"
        boolean endWithRBrace = StringUtils.endsWith(StringUtils.trim(currentLineText), "}");

        boolean emptyAndTabChar = StringUtils.isEmpty(StringUtils.trim(newText));

        if (emptyAndTabChar && (endWithLBrace || endWithRBrace || endsWithSemicolon)) {
            return VerifyResult.create(true);
        }
        return VerifyResult.create(false);
    }

    public static VerifyResult isValidChange(Editor editor, Document document, int newOffset, int previousOffset) {
        if (newOffset < 0 || previousOffset > newOffset) return VerifyResult.create(false);

        String addedText = document.getText(new TextRange(previousOffset, newOffset));
        if (isCodeReFormatAction(editor, document, newOffset, addedText)) {
            return VerifyResult.create(false);
        }

        if (isCopyCodeAction(editor, document, newOffset, addedText)) {
            return VerifyResult.create(false);
        }

        int currentLine = editor.getCaretModel().getLogicalPosition().line;
        String currentLogicalLineText = currentLine < 0 ? null : document.getText(
                new TextRange(document.getLineStartOffset(currentLine), document.getLineEndOffset(currentLine)));

        var language = LanguageUtil.getFileLanguage(FileDocumentManager.getInstance().getFile(document));
        VerifyResult result = ignoreLogicalPositionTrigger(addedText, currentLogicalLineText, language);
        if (result.isValid()) {
            return VerifyResult.create(!result.isValid(), result.getCompletionType());
        }
        if (!result.isValid() && !"comment".equals(result.completionType)) {
            int lineIndex = document.getLineNumber(newOffset);
            TextRange lineRange = TextRange.create(document.getLineStartOffset(lineIndex), document.getLineEndOffset(lineIndex));
            String currentLineText = document.getText(lineRange);
            result = ignoreFromCurrentLineChange(addedText, currentLineText, language);
            if (result.isValid()) {
                return VerifyResult.create(!result.isValid(), result.getCompletionType());
            }
        }

        boolean valid = isValidMidlinePosition(document, newOffset) &&
                isValidNonEmptyChange(addedText.length(), addedText) &&
                isSingleCharNonWhitespaceChange(addedText) &&
                !result.isValid();

        return VerifyResult.create(valid, result.getCompletionType());
    }

    private static boolean isCodeReFormatAction(Editor editor, Document document, int newOffset, String addedText) {
        if (!StringUtils.isEmpty(StringUtils.trim(addedText)) || StringUtils.startsWith(addedText, "\n")) {
            return false;
        }
        LogicalPosition logicalPosition = editor.getCaretModel().getLogicalPosition();
        int currentLine = logicalPosition.line;
        int lineStartOffset = document.getLineStartOffset(currentLine);
        int lineEndOffset = document.getLineEndOffset(currentLine);
        return newOffset < lineStartOffset || newOffset > lineEndOffset;
    }

    private static boolean isCopyCodeAction(Editor editor, Document document, int newOffset, String addedText) {
        if (StringUtils.isEmpty(StringUtils.trim(addedText))) {
            return false;
        }
        LogicalPosition logicalPosition = editor.getCaretModel().getLogicalPosition();
        int currentLine = logicalPosition.line;
        int lineStartOffset = document.getLineStartOffset(currentLine);
        int lineEndOffset = document.getLineEndOffset(currentLine);
        return newOffset < lineStartOffset || newOffset > lineEndOffset;
    }

    public static boolean checkTriggerTime(@NotNull Editor editor,
                                           int offset,
                                           @Nullable DevPilotCompletion lastShownSuggestion,
                                           @NotNull String userInput,
                                           @NotNull CompletionAdjustment completionAdjustment,
                                           String completionType) {
        boolean isConsistentWrite = System.currentTimeMillis() - lastTriggerInfo.getTriggerTime() < getTriggerInterval();
        Timer previousTimer = lastTriggerInfo.getTimer();
        if (previousTimer != null) {
            previousTimer.cancel();
        }
        if (isConsistentWrite) {
            buildDelayTrigger(editor, offset, lastShownSuggestion, userInput, completionAdjustment, completionType);
            return false;
        } else {
            lastTriggerInfo.setTriggerTime(System.currentTimeMillis());
            return true;
        }
    }

    private static int getTriggerInterval() {
        return CompletionSettingsState.getInstance().getInterval();
    }

    public static class TriggerInfo {

        private long triggerTime;

        private Timer timer;

        public Timer getTimer() {
            return timer;
        }

        public void setTimer(Timer timer) {
            this.timer = timer;
        }

        public long getTriggerTime() {
            return triggerTime;
        }

        public void setTriggerTime(long triggerTime) {
            this.triggerTime = triggerTime;
        }
    }

    private static void buildDelayTrigger(@NotNull Editor editor,
                                          int offset,
                                          @Nullable DevPilotCompletion lastShownSuggestion,
                                          @NotNull String userInput,
                                          @NotNull CompletionAdjustment completionAdjustment,
                                          String completionType) {
        lastTriggerInfo.setTriggerTime(System.currentTimeMillis());
        Timer timer = new Timer("delay-trigger");
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        singletonOfInlineCompletionHandler().retrieveAndShowCompletion(
                                editor,
                                offset,
                                lastShownSuggestion,
                                userInput,
                                completionAdjustment,
                                completionType);
                    }
                }, getTriggerInterval());
        lastTriggerInfo.setTimer(timer);
    }

    public static class VerifyResult {
        private boolean valid;

        // default is inline
        // type: comment, inline
        private String completionType = "inline";

        public VerifyResult(boolean valid) {
            this.valid = valid;
        }

        public VerifyResult(boolean valid, String completionType) {
            this.valid = valid;
            this.completionType = completionType;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getCompletionType() {
            return completionType;
        }

        public void setCompletionType(String completionType) {
            this.completionType = completionType;
        }

        public static VerifyResult create(boolean valid) {
            return new VerifyResult(valid);
        }

        public static VerifyResult create(boolean valid, String completionType) {
            return new VerifyResult(valid, completionType);
        }

        public static VerifyResult createComment(boolean valid) {
            return new VerifyResult(valid, "comment");
        }

    }
}











