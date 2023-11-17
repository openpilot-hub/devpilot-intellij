package com.zhongan.codeai.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.zhongan.codeai.enums.ModelTypeEnum;
import com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.settings.state.OpenAISettingsState;

public class SettingsStateTest extends BasePlatformTestCase {
    public void testOpenAISettings() {
        var settings = OpenAISettingsState.getInstance();
        settings.setModelBaseHost(ModelTypeEnum.TYQW.getName(), "https://test1.codeai.com");
        settings.setModelBaseHost(ModelTypeEnum.GPT3_5.getName(), "https://test2.codeai.com");
        assertEquals("https://test1.codeai.com", settings.getModelBaseHost(ModelTypeEnum.TYQW.getName()));
        assertEquals("https://test2.codeai.com", settings.getModelBaseHost(ModelTypeEnum.GPT3_5.getName()));
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

        assertEquals("{{selectedCode}}\nGiving the code above, please help to generate JUnit test cases for it, be aware that if the code is untestable, please state it and give suggestions instead.Put the code in code block.\n", settings.getDefaultActions().get("codeai.action.generate.tests"));
        assertEquals(6, settings.getDefaultActions().size());
    }

}
