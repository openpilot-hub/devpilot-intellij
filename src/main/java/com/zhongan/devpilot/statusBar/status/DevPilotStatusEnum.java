package com.zhongan.devpilot.statusBar.status;

import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.ThemeUtils;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;

public enum DevPilotStatusEnum {
    LoggedIn,
    NotLoggedIn,
    InCompletion,
    DISCONNECT_DARK;

    public @NotNull Icon getIcon() {
        switch (this) {
            case LoggedIn:
                return DevPilotIcons.SYSTEM_ICON_13;
            case InCompletion:
                return DevPilotIcons.COMPLETION_IN_PROGRESS;
            default:
                return ThemeUtils.isDarkTheme() ? DevPilotIcons.DISCONNECT_DARK : DevPilotIcons.DISCONNECT;
        }
    }

    public String getText() {
        switch (this) {
            case LoggedIn:
                return DevPilotMessageBundle.get("devpilot.status.loggedIn");
            case InCompletion:
                return DevPilotMessageBundle.get("devpilot.status.inCompletion");
            case DISCONNECT_DARK:
                return DevPilotMessageBundle.get("devpilot.notification.network.message");
            default:
                return DevPilotMessageBundle.get("devpilot.status.notLoggedIn");
        }
    }
}
