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
        assertEquals("User", settings.getFullName());

        settings.setFullName(null);
        System.setProperty("user.name", "Alice");
        assertEquals("Alice", settings.getFullName());

        settings.setFullName("Bob");
        assertEquals("Bob", settings.getFullName());
    }

    public void testEditorActionConfigurationState() {
        var settings = EditorActionConfigurationState.getInstance();

        assertEquals("{{selectedCode}}\nGiving the code above, please help to generate JUnit test cases for it, be aware that if the code is untestable, please state it and give suggestions instead:", settings.getDefaultActions().get("codeai.action.generate.tests"));
        assertEquals(6, settings.getDefaultActions().size());
    }

}
