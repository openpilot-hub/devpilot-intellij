package com.zhongan.devpilot.actions.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

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

}
