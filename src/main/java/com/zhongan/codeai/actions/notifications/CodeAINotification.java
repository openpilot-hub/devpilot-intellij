package com.zhongan.codeai.actions.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.zhongan.codeai.util.CodeAIMessageBundle;

public class CodeAINotification {

    public static void info(String content) {
        var notification = new Notification(
            "Open Pilot Notification Group",
            CodeAIMessageBundle.get("notification.group.codeai"),
            content,
            NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }

    public static void warn(String content) {
        var notification = new Notification(
            "Open Pilot Notification Group",
            CodeAIMessageBundle.get("notification.group.codeai"),
            content,
            NotificationType.WARNING);
        Notifications.Bus.notify(notification);
    }

    public static void error(String content) {
        var notification = new Notification(
            "Open Pilot Notification Group",
            CodeAIMessageBundle.get("notification.group.codeai"),
            content,
            NotificationType.ERROR);
        Notifications.Bus.notify(notification);
    }

}
