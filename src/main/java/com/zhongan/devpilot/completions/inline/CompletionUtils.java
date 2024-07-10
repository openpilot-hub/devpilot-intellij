package com.zhongan.devpilot.completions.inline;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.zhongan.devpilot.util.CommentUtil;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class CompletionUtils {
    private static final Pattern END_OF_LINE_VALID_PATTERN = Pattern.compile("^\\s*[)}\\]\"'`]*\\s*[:{;,]?\\s*$");

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

    // 代码-单行仅空字符或者仅换行忽略请求，注释-单行除换行全部忽略请求
    public static VerifyResult ignoreTrigger(String newText, String currentLineText, Language language) {
        boolean isPreComment = CommentUtil.containsComment(StringUtils.trim(currentLineText), language);

        // only contains empty and tab
        boolean emptyAndTabChar = StringUtils.isEmpty(StringUtils.trim(newText));
        boolean currentLineEmpty = StringUtils.isEmpty(StringUtils.trim(currentLineText));
        if (emptyAndTabChar && currentLineEmpty) {
            return VerifyResult.create(true);
        }

        boolean newlineChar = StringUtils.startsWith(newText, "\n");
        if (newlineChar && isPreComment) {
            return VerifyResult.createComment(false);
        }

        if (newlineChar || isPreComment) {
            return VerifyResult.create(true);
        }

        return VerifyResult.create(false);
    }

    public static VerifyResult isValidChange(Editor editor, Document document, int newOffset, int previousOffset) {
        if (newOffset < 0 || previousOffset > newOffset) return VerifyResult.create(false);
        String addedText = document.getText(new TextRange(previousOffset, newOffset));
        int currentLine = editor.getCaretModel().getLogicalPosition().line;
        String currentLineText = currentLine < 0 ? null : document.getText(
                new TextRange(document.getLineStartOffset(currentLine), document.getLineEndOffset(currentLine)));

        var language = LanguageUtil.getFileLanguage(FileDocumentManager.getInstance().getFile(document));
        VerifyResult result = ignoreTrigger(addedText, currentLineText, language);

        boolean valid = isValidMidlinePosition(document, newOffset) &&
                isValidNonEmptyChange(addedText.length(), addedText) &&
                isSingleCharNonWhitespaceChange(addedText) &&
                !result.isValid();

        return VerifyResult.create(valid, result.getCompletionType());
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











