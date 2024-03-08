package com.zhongan.devpilot.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.zhongan.devpilot.actions.editor.popupmenu.PopupMenuEditorActionGroupUtil;
import com.zhongan.devpilot.enums.ModelServiceEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.ConfigChangeUtils;
import com.zhongan.devpilot.util.ConfigurableUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import javax.swing.JComponent;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import static com.zhongan.devpilot.enums.ModelServiceEnum.AIGATEWAY;

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
        var aiGatewaySettings = AIGatewaySettingsState.getInstance();
        var languageSettings = LanguageSettingsState.getInstance();
        var serviceForm = settingsComponent.getDevPilotConfigForm();
        var selectedSso = serviceForm.getSelectedZaSso();
        var completionEnable = CompletionSettingsState.getInstance().getEnable();

        return !settingsComponent.getFullName().equals(settings.getFullName())
                || !serviceForm.getLanguageIndex().equals(languageSettings.getLanguageIndex())
                || !selectedSso.getName().equals(aiGatewaySettings.getSelectedSso())
                || !settingsComponent.getCompletionEnabled() == (completionEnable);
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

        var serviceForm = settingsComponent.getDevPilotConfigForm();
        var selectedModel = serviceForm.getSelectedModel();

        CompletionSettingsState completionSettings = CompletionSettingsState.getInstance();
        completionSettings.setEnable(settingsComponent.getCompletionEnabled());
        checkCodeCompletionConfig(selectedModel);
    }

    @Override
    public void dispose() {
    }

    private void checkCodeCompletionConfig(ModelServiceEnum serviceEnum) {
        if (!AIGATEWAY.equals(serviceEnum) && CompletionSettingsState.getInstance().getEnable()) {
            CompletionSettingsState.getInstance().setEnable(false);
        }
    }

}
