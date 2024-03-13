package com.zhongan.devpilot.actions.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import org.apache.commons.lang3.StringUtils;

public class DevPilotNotification {

    public static void info(String content) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }

    public static void warn(String content) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.WARNING);
        Notifications.Bus.notify(notification);
    }

    public static void error(String content) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.ERROR);
        Notifications.Bus.notify(notification);
    }

    public static void debug(String content) {
        String selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        String host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);
        if (StringUtils.endsWith(host, "dev") || StringUtils.endsWith(host, "prd")) {
            Notification notification = new Notification("DevPilot Notification Group", DevPilotMessageBundle.get("notification.group.devpilot"), content, NotificationType.ERROR);
            Notifications.Bus.notify(notification);
        }

    }

}
