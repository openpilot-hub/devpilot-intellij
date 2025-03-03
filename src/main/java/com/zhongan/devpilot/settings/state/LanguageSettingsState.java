package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "DevPilot_LanguageSettings", storages = @Storage("DevPilot_LanguageSettings.xml"))
public class LanguageSettingsState implements PersistentStateComponent<LanguageSettingsState> {

    private Integer languageIndex = 1;

    private Integer gitLogLanguageIndex = 0;

    public static LanguageSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(LanguageSettingsState.class);
    }

    public Integer getLanguageIndex() {
        return languageIndex;
    }

    public void setLanguageIndex(Integer languageIndex) {
        this.languageIndex = languageIndex;
    }

    public Integer getGitLogLanguageIndex() {
        return gitLogLanguageIndex;
    }

    public void setGitLogLanguageIndex(Integer gitLogLanguageIndex) {
        this.gitLogLanguageIndex = gitLogLanguageIndex;
    }

    @Override
    public LanguageSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(LanguageSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
