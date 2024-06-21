package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "DevPilot_TrialServiceSettings", storages = @Storage("DevPilot_TrialServiceSettings.xml"))
public class TrialServiceSettingsState implements PersistentStateComponent<TrialServiceSettingsState> {
    private String wxToken;

    private String wxUsername;

    private String wxUserId;

    public static TrialServiceSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(TrialServiceSettingsState.class);
    }

    public String getWxToken() {
        return wxToken;
    }

    public void setWxToken(String wxToken) {
        this.wxToken = wxToken;
    }

    public String getWxUsername() {
        return wxUsername;
    }

    public void setWxUsername(String wxUsername) {
        this.wxUsername = wxUsername;
    }

    public String getWxUserId() {
        return wxUserId;
    }

    public void setWxUserId(String wxUserId) {
        this.wxUserId = wxUserId;
    }

    @Override
    public TrialServiceSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(TrialServiceSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
