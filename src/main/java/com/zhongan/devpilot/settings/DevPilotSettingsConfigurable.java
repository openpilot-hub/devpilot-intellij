package com.zhongan.devpilot.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.zhongan.devpilot.actions.editor.popupmenu.PopupMenuEditorActionGroupUtil;
import com.zhongan.devpilot.enums.LoginTypeEnum;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.AvailabilityCheck;
import com.zhongan.devpilot.util.ConfigChangeUtils;
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
        var languageSettings = LanguageSettingsState.getInstance();
        var languageIndex = settingsComponent.getLanguageIndex();
        var completionEnable = CompletionSettingsState.getInstance().getEnable();
        Boolean enable = AvailabilityCheck.getInstance().getEnable();

        return !settingsComponent.getFullName().equals(settings.getFullName())
                || !languageIndex.equals(languageSettings.getLanguageIndex())
                || !settingsComponent.getCompletionEnabled() == (completionEnable)
                || !settingsComponent.getStatusCheckEnabled() == (enable);
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

        CompletionSettingsState completionSettings = CompletionSettingsState.getInstance();
        completionSettings.setEnable(settingsComponent.getCompletionEnabled());

        AvailabilityCheck availabilityCheck = AvailabilityCheck.getInstance();
        availabilityCheck.setEnable(settingsComponent.getStatusCheckEnabled());

        checkCodeCompletionConfig(LoginTypeEnum.getLoginTypeEnum(settings.getLoginType()));
    }

    @Override
    public void dispose() {
    }

    private void checkCodeCompletionConfig(LoginTypeEnum loginType) {
        if (!(LoginTypeEnum.ZA.equals(loginType) || LoginTypeEnum.ZA_TI.equals(loginType))
                && CompletionSettingsState.getInstance().getEnable()) {
            CompletionSettingsState.getInstance().setEnable(false);
        }
    }

}
