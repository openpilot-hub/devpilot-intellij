package com.zhongan.devpilot.actions.notifications;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.zhongan.devpilot.settings.DevPilotSettingsConfigurable;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.statusBar.DevPilotStatusBarBaseWidget;
import com.zhongan.devpilot.statusBar.status.DevPilotStatusEnum;
import com.zhongan.devpilot.update.DevPilotUpdate;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.LoginUtils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static void linkInfo(String content, String text, String url) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.INFORMATION);

        notification.addAction(new NotificationAction(text) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                BrowserUtil.browse(url);
            }
        });

        Notifications.Bus.notify(notification);
    }

    public static void infoAndAction(String content, String display, String url) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.INFORMATION);
        notification.addAction(new NotificationAction(display) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent, @NotNull Notification notification) {
                BrowserUtil.browse(url);
            }
        });
        Notifications.Bus.notify(notification);
    }

    public static void debug(String content) {
        var selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        var host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);
        if (StringUtils.endsWith(host, "dev") || StringUtils.endsWith(host, "prd")) {
            var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                content,
                NotificationType.ERROR);
            Notifications.Bus.notify(notification);
        }
    }

    public static void updateNotification(Project project) {
        var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                DevPilotMessageBundle.get("devpilot.notification.update.message"),
                NotificationType.IDE_UPDATE);
        notification.addAction(NotificationAction
                .createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.installButton"), () -> {
                    ApplicationManager.getApplication()
                            .executeOnPooledThread(() -> DevPilotUpdate.installUpdate(project));
                }));
        notification.addAction(NotificationAction
                .createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.hideButton"), () -> {
                }));
        Notifications.Bus.notify(notification);
    }

    public static void networkDownNotification(Project project) {
        var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                DevPilotMessageBundle.get("devpilot.notification.network.message"),
                NotificationType.ERROR);
        DevPilotStatusBarBaseWidget.update(project, DevPilotStatusEnum.Disconnected);
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.network.setting"),
                        () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, DevPilotSettingsConfigurable.class)));
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.hideButton"), () -> {

        }));
        Notifications.Bus.notify(notification);
    }

    public static void simpleNotLoginNotification(@Nullable Project project) {
        var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                DevPilotMessageBundle.get("devpilot.status.notLoggedIn"),
                NotificationType.WARNING);
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.settings.service.statusbar.login.desc"),
                () -> ApplicationManager.getApplication().executeOnPooledThread(LoginUtils::gotoLogin)));
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.hideButton"), () -> {

        }));
        Notifications.Bus.notify(notification, project);
    }

    public static void notLoginNotification(Project project) {
        var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                DevPilotMessageBundle.get("devpilot.status.notLoggedIn"),
                NotificationType.WARNING);
        DevPilotStatusBarBaseWidget.update(project, DevPilotStatusEnum.NotLoggedIn);
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.settings.service.statusbar.login.desc"),
                () -> ApplicationManager.getApplication().executeOnPooledThread(LoginUtils::gotoLogin)));
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.hideButton"), () -> {

        }));
        Notifications.Bus.notify(notification);
    }

    public static void upgradePluginNotification(Project project) {
        var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                DevPilotMessageBundle.get("devpilot.notification.version.message"),
                NotificationType.INFORMATION);
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.upgrade.message"),
                () -> ShowSettingsUtil.getInstance().showSettingsDialog(ProjectUtil.currentOrDefaultProject(project), "Plugins")));
        notification.addAction(NotificationAction.createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.hideButton"), () -> {

        }));

        Notifications.Bus.notify(notification);
    }

    public static void infoAndSetting(Project project, String display, String text) {
        var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                text,
                NotificationType.INFORMATION);
        notification.addAction(new NotificationAction(display) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, DevPilotSettingsConfigurable.class);
            }
        });
        Notifications.Bus.notify(notification);
    }
}
