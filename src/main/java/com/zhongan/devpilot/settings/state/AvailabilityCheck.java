package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "DevPilot_AvailabilityCheckSettings", storages = @Storage("DevPilot_AvailabilityCheckSettings.xml"))
public class AvailabilityCheck implements PersistentStateComponent<AvailabilityCheck> {

    private Boolean enable = true;

    public static AvailabilityCheck getInstance() {
        return ApplicationManager.getApplication().getService(AvailabilityCheck.class);
    }

    public Boolean getEnable() {
        return enable == null || enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public AvailabilityCheck getState() {
        return this;
    }

    @Override
    public void loadState(AvailabilityCheck state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}

