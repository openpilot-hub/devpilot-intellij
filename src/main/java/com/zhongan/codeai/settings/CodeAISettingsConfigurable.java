package com.zhongan.codeai.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.settings.state.OpenAISettingsState;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import javax.swing.JComponent;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class CodeAISettingsConfigurable implements Configurable, Disposable {

    private CodeAISettingsComponent settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return CodeAIMessageBundle.get("codeai.settins");
    }

    @Override
    public @Nullable JComponent createComponent() {
        var settings = CodeAILlmSettingsState.getInstance();
        settingsComponent = new CodeAISettingsComponent(this, settings);
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = CodeAILlmSettingsState.getInstance();
        var openAISettings = OpenAISettingsState.getInstance();
        var serviceForm = settingsComponent.getCodeAIConfigForm();
        return !settingsComponent.getFullName().equals(settings.getFullName()) || !serviceForm.getOpenAIBaseHost().equals(openAISettings.getOpenAIBaseHost());
    }

    @Override
    public void apply() throws ConfigurationException {
        var settings = CodeAILlmSettingsState.getInstance();
        settings.setFullName(settingsComponent.getFullName());

        var openAISettings = OpenAISettingsState.getInstance();
        var serviceForm = settingsComponent.getCodeAIConfigForm();
        openAISettings.setOpenAIBaseHost(serviceForm.getOpenAIBaseHost());
    }

    @Override
    public void dispose() {
    }

}
