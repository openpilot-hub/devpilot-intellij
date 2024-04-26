package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
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
    public static boolean ignoreTrigger(String newText, String currentLineText) {
        boolean isPreComment = CommentUtil.containsComment(currentLineText);

        // only contains empty and tab
        boolean emptyAndTabChar = StringUtils.isEmpty(StringUtils.trim(newText));
        boolean currentLineEmpty = StringUtils.isEmpty(StringUtils.trim(currentLineText));
        if (emptyAndTabChar && currentLineEmpty) {
            return true;
        }

        boolean newlineChar = StringUtils.startsWith(newText, "\n");
        if (newlineChar && !isPreComment) {
            return true;
        }

        return !newlineChar && isPreComment;
    }

    public static boolean isValidChange(Editor editor, Document document, int newOffset, int previousOffset) {
        if (newOffset < 0 || previousOffset > newOffset) return false;
        String addedText = document.getText(new TextRange(previousOffset, newOffset));
        int currentLine = editor.getCaretModel().getLogicalPosition().line;
        String currentLineText = currentLine < 0 ? null : document.getText(
            new TextRange(document.getLineStartOffset(currentLine), document.getLineEndOffset(currentLine)));
        return
            isValidMidlinePosition(document, newOffset) &&
                isValidNonEmptyChange(addedText.length(), addedText) &&
                isSingleCharNonWhitespaceChange(addedText) &&
                !ignoreTrigger(addedText, currentLineText);
    }

}











