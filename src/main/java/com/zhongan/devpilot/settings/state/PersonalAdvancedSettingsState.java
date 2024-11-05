package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "DevPilot_Personal_Advanced_Settings", storages = @Storage("DevPilot_Personal_Advanced_Settings.xml"))
public class PersonalAdvancedSettingsState implements PersistentStateComponent<PersonalAdvancedSettingsState> {

    private String localStorage;

    public static PersonalAdvancedSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(PersonalAdvancedSettingsState.class);
    }

    @Override
    public PersonalAdvancedSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(PersonalAdvancedSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getLocalStorage() {
        return localStorage;
    }

    public void setLocalStorage(String localStorage) {
        this.localStorage = localStorage;
    }
}
