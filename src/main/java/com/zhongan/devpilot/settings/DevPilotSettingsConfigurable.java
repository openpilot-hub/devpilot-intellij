package com.zhongan.devpilot.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.zhongan.devpilot.actions.editor.popupmenu.PopupMenuEditorActionGroupUtil;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.OpenAISettingsState;
import com.zhongan.devpilot.util.ConfigurableUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import javax.swing.JComponent;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

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
        ConfigurableUtils.setConfigurableCache(settingsComponent);
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = DevPilotLlmSettingsState.getInstance();
        var openAISettings = OpenAISettingsState.getInstance();
        var aiGatewaySettings = AIGatewaySettingsState.getInstance();
        var languageSettings = LanguageSettingsState.getInstance();
        var codeLlamaSettings = CodeLlamaSettingsState.getInstance();
        var serviceForm = settingsComponent.getDevPilotConfigForm();
        var selectedModel = serviceForm.getSelectedModel();
        var selectedModelType = serviceForm.getAIGatewayModel();
        var selectedSso = serviceForm.getSelectedZaSso();

        return !settingsComponent.getFullName().equals(settings.getFullName())
                || !selectedModel.getName().equals(settings.getSelectedModel())
                || !selectedModelType.getName().equals(aiGatewaySettings.getSelectedModel())
                || !serviceForm.getOpenAIBaseHost().equals(openAISettings.getModelHost())
                || !serviceForm.getAIGatewayBaseHost().equals(aiGatewaySettings.getModelBaseHost(selectedModelType.getName()))
                || !serviceForm.getOpenAIKey().equals(openAISettings.getPrivateKey())
                || !serviceForm.getCodeLlamaBaseHost().equals(codeLlamaSettings.getModelHost())
                || !serviceForm.getLanguageIndex().equals(languageSettings.getLanguageIndex())
                || !selectedSso.getName().equals(aiGatewaySettings.getSelectedSso());
    }

    @Override
    public void apply() throws ConfigurationException {
        var settings = DevPilotLlmSettingsState.getInstance();
        settings.setFullName(settingsComponent.getFullName());

        var languageSettings = LanguageSettingsState.getInstance();
        Integer languageIndex = settingsComponent.getLanguageIndex();
        languageSettings.setLanguageIndex(languageIndex);

        PopupMenuEditorActionGroupUtil.refreshActions(null);

        var openAISettings = OpenAISettingsState.getInstance();
        var aiGatewaySettings = AIGatewaySettingsState.getInstance();
        var codeLlamaSettings = CodeLlamaSettingsState.getInstance();
        var serviceForm = settingsComponent.getDevPilotConfigForm();
        var selectedModel = serviceForm.getSelectedModel();
        var selectedModelType = serviceForm.getAIGatewayModel();
        var selectedSso = serviceForm.getSelectedZaSso();

        settings.setSelectedModel(selectedModel.getName());
        openAISettings.setModelHost(serviceForm.getOpenAIBaseHost());
        openAISettings.setPrivateKey(serviceForm.getOpenAIKey());
        codeLlamaSettings.setModelHost(serviceForm.getCodeLlamaBaseHost());
        aiGatewaySettings.setModelBaseHost(selectedModelType.getName(), serviceForm.getAIGatewayBaseHost());
        aiGatewaySettings.setSelectedModel(selectedModelType.getName());
        aiGatewaySettings.setSelectedSso(selectedSso.getName());
    }

    @Override
    public void dispose() {
    }

}
