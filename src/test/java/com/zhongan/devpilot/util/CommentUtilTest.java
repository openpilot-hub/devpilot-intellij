package com.zhongan.devpilot.util;

import com.intellij.lang.Language;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CommentUtilTest extends BasePlatformTestCase {
    public void testCommentJava() {
        var language = Language.findLanguageByID("JAVA");
        assertTrue(CommentUtil.containsComment("/* this is a block comment */", language));
        assertTrue(CommentUtil.containsComment("// this is a single line comment", language));
        assertFalse(CommentUtil.containsComment("this is not a comment", language));
    }
}
