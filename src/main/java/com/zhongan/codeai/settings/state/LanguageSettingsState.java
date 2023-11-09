package com.zhongan.codeai.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "OpenPilot_LanguageSettings", storages = @Storage("OpenPilot_LanguageSettings.xml"))
public class LanguageSettingsState implements PersistentStateComponent<LanguageSettingsState> {

    private static Integer languageIndex = 1;

    public static LanguageSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(LanguageSettingsState.class);
    }

    public Integer getLanguageIndex() {
        return languageIndex;
    }

    public void setLanguageIndex(Integer languageIndex) {
        this.languageIndex = languageIndex;
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
