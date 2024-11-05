
package com.zhongan.devpilot.util;

import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;

public class CommentUtil {
    public static boolean isMultiLineComment(String text, Language language) {
        final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);

        if (commenter == null) {
            return false;
        }

        var blockSuffix = commenter.getBlockCommentSuffix();
        return blockSuffix != null && text.endsWith(blockSuffix);
    }

    public static boolean isSingleLineComment(String text, Language language) {
        final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);

        if (commenter == null) {
            return false;
        }

        var linePrefix = commenter.getLineCommentPrefix();
        return linePrefix != null && text.startsWith(linePrefix);
    }

    public static boolean containsComment(String text, Language language) {
        if (text == null || language == null) return false;
        return isMultiLineComment(text, language) || isSingleLineComment(text, language);
    }

}