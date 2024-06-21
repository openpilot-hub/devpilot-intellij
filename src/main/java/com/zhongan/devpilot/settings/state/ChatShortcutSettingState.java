package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "DevPilot_ChatShortcutSetting", storages = @Storage("DevPilot_ChatShortcutSetting.xml"))
public class ChatShortcutSettingState implements PersistentStateComponent<ChatShortcutSettingState> {

    private Boolean enable = true;

    public static ChatShortcutSettingState getInstance() {
        return ApplicationManager.getApplication().getService(ChatShortcutSettingState.class);
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public @Nullable ChatShortcutSettingState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ChatShortcutSettingState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
