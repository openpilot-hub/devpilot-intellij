package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "DevPilot_LocalRagSettings", storages = @Storage("DevPilot_LocalRagSettings.xml"))
public class LocalRagSettingsState implements PersistentStateComponent<LocalRagSettingsState> {
    private Boolean enable = true;

    public static LocalRagSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(LocalRagSettingsState.class);
    }

    @Override
    public @Nullable LocalRagSettingsState getState() {
        return this;
    }

    public Boolean getEnable() {
        return enable == null || enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public void loadState(@NotNull LocalRagSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
