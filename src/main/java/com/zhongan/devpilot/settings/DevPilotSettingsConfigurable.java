package com.zhongan.devpilot.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.zhongan.devpilot.actions.editor.popupmenu.PopupMenuEditorActionGroupUtil;
import com.zhongan.devpilot.settings.state.*;
import com.zhongan.devpilot.util.ConfigChangeUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DevPilotSettingsConfigurable implements Configurable, Disposable {

    private DevPilotSettingsComponent settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return DevPilotMessageBundle.get("devpilot.settings");
    }

    @Override
    public @Nullable JComponent createComponent() {
        var settings = DevPilotLlmSettingsState.getInstance();
        settingsComponent = new DevPilotSettingsComponent(this, settings);
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = DevPilotLlmSettingsState.getInstance();
        var openAISettings = OpenAISettingsState.getInstance();
        var aiGatewaySettings = AIGatewaySettingsState.getInstance();
        var languageSettings = LanguageSettingsState.getInstance();
        var codeLlamaSettings = CodeLlamaSettingsState.getInstance();
        var ollamaSettings = OllamaSettingsState.getInstance();
        var serviceForm = settingsComponent.getDevPilotConfigForm();
        var selectedModel = serviceForm.getSelectedModel();
        var selectedModelType = serviceForm.getAIGatewayModel();

        return !settingsComponent.getFullName().equals(settings.getFullName())
                || !selectedModel.getName().equals(settings.getSelectedModel())
                || !selectedModelType.getName().equals(aiGatewaySettings.getSelectedModel())
                || !serviceForm.getOpenAIBaseHost().equals(openAISettings.getModelHost())
                || !serviceForm.getOpenAIModelName().getName().equals(openAISettings.getModelName())
                || !serviceForm.getOpenAICustomModelName().equals(openAISettings.getCustomModelName())
                || !serviceForm.getAIGatewayBaseHost().equals(aiGatewaySettings.getModelBaseHost(selectedModelType.getName()))
                || !serviceForm.getOpenAIKey().equals(openAISettings.getPrivateKey())
                || !serviceForm.getCodeLlamaBaseHost().equals(codeLlamaSettings.getModelHost())
                || !serviceForm.getCodeLlamaModelName().equals(codeLlamaSettings.getModelName())
                || !serviceForm.getOllamaModelName().equals(ollamaSettings.getModelName())
                || !serviceForm.getOllamaBaseHost().equals(ollamaSettings.getModelHost())
                || !serviceForm.getLanguageIndex().equals(languageSettings.getLanguageIndex());
    }

    @Override
    public void apply() throws ConfigurationException {
        var settings = DevPilotLlmSettingsState.getInstance();
        settings.setFullName(settingsComponent.getFullName());

        var languageSettings = LanguageSettingsState.getInstance();
        Integer languageIndex = settingsComponent.getLanguageIndex();

        // if language changed, refresh webview
        if (!languageIndex.equals(languageSettings.getLanguageIndex())) {
            ConfigChangeUtils.localeChanged(languageIndex);
        }

        languageSettings.setLanguageIndex(languageIndex);

        PopupMenuEditorActionGroupUtil.refreshActions(null);

        var openAISettings = OpenAISettingsState.getInstance();
        var aiGatewaySettings = AIGatewaySettingsState.getInstance();
        var codeLlamaSettings = CodeLlamaSettingsState.getInstance();
        var ollamaSettings = OllamaSettingsState.getInstance();
        var serviceForm = settingsComponent.getDevPilotConfigForm();
        var selectedModel = serviceForm.getSelectedModel();
        var selectedModelType = serviceForm.getAIGatewayModel();
        var openAIModelName = serviceForm.getOpenAIModelName();
        String ollamaBaseHost = serviceForm.getOllamaBaseHost();
        String ollamaModelName = serviceForm.getOllamaModelName();

        settings.setSelectedModel(selectedModel.getName());
        openAISettings.setModelHost(serviceForm.getOpenAIBaseHost());
        openAISettings.setPrivateKey(serviceForm.getOpenAIKey());
        openAISettings.setModelName(openAIModelName.getName());
        openAISettings.setCustomModelName(serviceForm.getOpenAICustomModelName());
        codeLlamaSettings.setModelHost(serviceForm.getCodeLlamaBaseHost());
        codeLlamaSettings.setModelName(serviceForm.getCodeLlamaModelName());
        aiGatewaySettings.setModelBaseHost(selectedModelType.getName(), serviceForm.getAIGatewayBaseHost());
        aiGatewaySettings.setSelectedModel(selectedModelType.getName());
        ollamaSettings.setModelHost(ollamaBaseHost);
        ollamaSettings.setModelName(ollamaModelName);
    }

    @Override
    public void dispose() {
    }

}
