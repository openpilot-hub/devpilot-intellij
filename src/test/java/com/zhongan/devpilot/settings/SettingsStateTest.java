package com.zhongan.devpilot.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.ModelServiceEnum;
import com.zhongan.devpilot.settings.actionconfiguration.EditorActionConfigurationState;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.OpenAISettingsState;

public class SettingsStateTest extends BasePlatformTestCase {
    public void testOpenAISettings() {
        var settings = OpenAISettingsState.getInstance();
        settings.setModelHost("https://test1.devpilot.com");
        settings.setPrivateKey("privateKey");
        assertEquals("https://test1.devpilot.com", settings.getModelHost());
        assertEquals("privateKey", settings.getPrivateKey());
    }

    public void testCodeLlamaSettings() {
        var settings = CodeLlamaSettingsState.getInstance();
        settings.setModelHost("https://test2.devpilot.com");
        assertEquals("https://test2.devpilot.com", settings.getModelHost());
    }

    public void testDevPilotLlmSettings() {
        var settings = DevPilotLlmSettingsState.getInstance();
        settings.setFullName(null);
        System.setProperty("user.name", "Alice");
        assertEquals("Alice", settings.getFullName());

        settings.setFullName("Bob");
        assertEquals("Bob", settings.getFullName());

        settings.setFullName(null);
        assertEquals("Alice", settings.getFullName());

        settings.setSelectedModel(ModelServiceEnum.OPENAI.getName());
        assertEquals(ModelServiceEnum.OPENAI.getName(), settings.getSelectedModel());
    }

}
