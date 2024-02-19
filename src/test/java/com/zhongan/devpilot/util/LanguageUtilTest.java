package com.zhongan.devpilot.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class LanguageUtilTest {

    @Test
    public void getFileSuffixByLanguage() {
        assertEquals(".java", LanguageUtil.getFileSuffixByLanguage(""));
        assertEquals(".java", LanguageUtil.getFileSuffixByLanguage("Java"));
        assertEquals(".java", LanguageUtil.getFileSuffixByLanguage("java"));
        assertEquals(".kt", LanguageUtil.getFileSuffixByLanguage("Kotlin"));
        assertEquals(".groovy", LanguageUtil.getFileSuffixByLanguage("groovy"));
    }

    @Test
    public void getLanguageByName() {
        LanguageUtil.Language language = LanguageUtil.getLanguageByName("Java");
        assertNotNull(language);
        assertEquals("Java", language.getLanguageName());
        assertEquals("java", language.getDefaultFileExtension());
        assertEquals("JUnit4", language.getDefaultTestFramework());
        assertEquals("Mockito", language.getDefaultMockFramework());
    }

    @Test
    public void getLanguageByExtension() {
        LanguageUtil.Language language = LanguageUtil.getLanguageByExtension("java");
        assertNotNull(language);
        assertEquals("Java", language.getLanguageName());
        assertEquals("java", language.getDefaultFileExtension());
        assertEquals("JUnit4", language.getDefaultTestFramework());
        assertEquals("Mockito", language.getDefaultMockFramework());
    }

}