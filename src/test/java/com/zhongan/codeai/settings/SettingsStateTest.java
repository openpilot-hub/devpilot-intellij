package com.zhongan.codeai.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.settings.state.OpenAISettingsState;

public class SettingsStateTest extends BasePlatformTestCase {
    public void testOpenAISettings() {
        var settings = OpenAISettingsState.getInstance();
        settings.setOpenAIBaseHost("https://test.codeai.com");
        assertEquals("https://test.codeai.com", settings.getOpenAIBaseHost());
    }

    public void testCodeAILlmSettings() {
        var settings = CodeAILlmSettingsState.getInstance();
        assertEquals("CodeAI", settings.getFullName());

        settings.setFullName(null);
        System.setProperty("user.name", "Alice");
        assertEquals("Alice", settings.getFullName());

        settings.setFullName("Bob");
        assertEquals("Bob", settings.getFullName());
    }

    public void testEditorActionConfigurationState() {
        var settings = EditorActionConfigurationState.getInstance();
        assertEquals("Generate tests for the selected code {{selectedCode}}", settings.getDefaultActions().get("Generate Tests"));
        assertEquals(5, settings.getDefaultActions().size());
    }
}
