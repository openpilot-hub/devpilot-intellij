
package com.zhongan.devpilot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentUtil {

    private static final String MULTI_LINE_REGEX = "/\\*(.|\\n)*?\\*/";

    private static final String SINGLE_LINE_REGEX = "//.*";

    private static final Pattern MULTI_LINE_PATTERN = Pattern.compile(MULTI_LINE_REGEX);

    private static final Pattern SINGLE_LINE_PATTERN = Pattern.compile(SINGLE_LINE_REGEX);

    public static boolean isMultiLineComment(String text) {
        Matcher multiLineCommentMatcher = MULTI_LINE_PATTERN.matcher(text);

        while (multiLineCommentMatcher.find()) {
            return true;
        }
        return false;
    }

    public static boolean isSingleLineComment(String text) {
        Matcher singleLineCommentMatcher = SINGLE_LINE_PATTERN.matcher(text);

        while (singleLineCommentMatcher.find()) {
            return true;
        }
        return false;
    }

    public static boolean containsComment(String text) {
        if (text == null) return false;
        return isMultiLineComment(text) || isSingleLineComment(text);
    }

}