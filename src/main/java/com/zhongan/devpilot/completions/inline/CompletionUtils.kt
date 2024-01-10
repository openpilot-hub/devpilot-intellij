package com.zhongan.devpilot.completions.inline

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import java.util.regex.Pattern

object CompletionUtils {
    private val END_OF_LINE_VALID_PATTERN = Pattern.compile("^\\s*[)}\\]\"'`]*\\s*[:{;,]?\\s*$")

    @JvmStatic
    fun isValidDocumentChange(document: Document, newOffset: Int, previousOffset: Int): Boolean {
        if (newOffset < 0 || previousOffset > newOffset) return false

        val addedText = document.getText(TextRange(previousOffset, newOffset))
        return (
                isValidMidlinePosition(document, newOffset) &&
                        isValidNonEmptyChange(addedText.length, addedText) &&
                        isSingleCharNonWhitespaceChange(addedText)
                )
    }

    @JvmStatic
    fun isValidMidlinePosition(document: Document, offset: Int): Boolean {
        val lineIndex: Int = document.getLineNumber(offset)
        val lineRange = TextRange.create(document.getLineStartOffset(lineIndex), document.getLineEndOffset(lineIndex))
        val line = document.getText(lineRange)
        val lineSuffix = line.substring(offset - lineRange.startOffset)
        return END_OF_LINE_VALID_PATTERN.matcher(lineSuffix).matches()
    }

    @JvmStatic
    fun isValidNonEmptyChange(replacedTextLength: Int, newText: String): Boolean {
        return replacedTextLength >= 0 && newText != ""
    }

    @JvmStatic
    fun isSingleCharNonWhitespaceChange(newText: String): Boolean {
        return newText.trim().length <= 1
    }
}
