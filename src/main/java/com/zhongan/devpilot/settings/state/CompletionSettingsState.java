package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "DevPilot_CompletionSettings", storages = @Storage("DevPilot_CompletionSettings.xml"))
public class CompletionSettingsState implements PersistentStateComponent<CompletionSettingsState> {

    private Boolean enable = true;

    private Integer interval = 500; // ms

    public static CompletionSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(CompletionSettingsState.class);
    }

    public Boolean getEnable() {
        return enable == null ? true : enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        if (interval >= 500) {
            this.interval = interval;
        }
    }

    @Override
    public CompletionSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(CompletionSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}

