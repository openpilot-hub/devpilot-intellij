package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "DevPilot_StatusCheckSettings", storages = @Storage("DevPilot_StatusCheckSettings.xml"))
public class StatusCheckSettingsState implements PersistentStateComponent<StatusCheckSettingsState> {

    private Boolean enable = true;

    public static StatusCheckSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(StatusCheckSettingsState.class);
    }

    public Boolean getEnable() {
        return enable == null || enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public StatusCheckSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(StatusCheckSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}

