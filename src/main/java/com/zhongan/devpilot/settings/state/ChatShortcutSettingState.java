package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "DevPilot_ChatShortcutSetting", storages = @Storage("DevPilot_ChatShortcutSetting.xml"))
public class ChatShortcutSettingState implements PersistentStateComponent<ChatShortcutSettingState> {

    private Boolean enable = true;

    private static Integer displayIndex = 1;

    public static ChatShortcutSettingState getInstance() {
        return ApplicationManager.getApplication().getService(ChatShortcutSettingState.class);
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Integer getDisplayIndex() {
        return displayIndex;
    }

    public void setDisplayIndex(Integer displayIndex) {
        if (NumberUtils.INTEGER_ZERO.equals(displayIndex)) {
            enable = Boolean.FALSE;
        } else {
            enable = Boolean.TRUE;
        }
        ChatShortcutSettingState.displayIndex = displayIndex;
    }

    public boolean isInlineDisplay() {
        return NumberUtils.INTEGER_ONE.equals(displayIndex);
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
