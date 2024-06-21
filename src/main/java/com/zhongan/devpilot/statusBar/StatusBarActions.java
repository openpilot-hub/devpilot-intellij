package com.zhongan.devpilot.statusBar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.Consumer;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.settings.DevPilotSettingsConfigurable;
import com.zhongan.devpilot.settings.state.ChatShortcutSettingState;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;

public class StatusBarActions {

    public static DefaultActionGroup buildStatusBarActionsGroup() {
        List<AnAction> actions = new ArrayList<>();
        if (LoginUtils.isLogin()) {
            actions.add(createAccountShowAction());
        }
        actions.add(createLoginAction());
        actions.add(createEditSettingsAction());
        actions.add(createChatShortcutSwitchAction());
        actions.add(createCompletionsAction());
        return new DefaultActionGroup(actions);
    }

    private static DumbAwareAction createCompletionsAction() {
        if (CompletionSettingsState.getInstance().getEnable()) {
            return DumbAwareAction.create(
                DevPilotMessageBundle.get("devpilot.settings.service.code.completion.disabled.desc"),
                event -> CompletionSettingsState.getInstance().setEnable(false)
            );
        } else {
            return DumbAwareAction.create(
                DevPilotMessageBundle.get("devpilot.settings.service.code.completion.enable.desc"),
                event -> CompletionSettingsState.getInstance().setEnable(true)
            );
        }
    }

    private static DumbAwareAction createLoginAction() {
        if (LoginUtils.isLogin()) {
            return createActionWithIcon(
                    DevPilotMessageBundle.get("devpilot.settings.service.statusbar.logout.desc"),
                    ThemeUtils.isDarkTheme() ? DevPilotIcons.LOGOUT_DARK : DevPilotIcons.LOGOUT,
                    event -> LoginUtils.logout(), true
            );
        } else {
            return createActionWithIcon(
                    DevPilotMessageBundle.get("devpilot.settings.service.statusbar.login.desc"),
                    ThemeUtils.isDarkTheme() ? DevPilotIcons.LOGIN_DARK : DevPilotIcons.LOGIN,
                    event -> LoginUtils.gotoLogin(), true);
        }
    }

    private static DumbAwareAction createChatShortcutSwitchAction() {
        if (ChatShortcutSettingState.getInstance().getEnable()) {
            return DumbAwareAction.create(
                DevPilotMessageBundle.get("devpilot.settings.service.chat.shortcut.disabled.desc"),
                event -> ChatShortcutSettingState.getInstance().setEnable(false)
            );
        } else {
            return DumbAwareAction.create(
                DevPilotMessageBundle.get("devpilot.settings.service.chat.shortcut.enable.desc"),
                event -> ChatShortcutSettingState.getInstance().setEnable(true)
            );
        }
    }

    private static DumbAwareAction createEditSettingsAction() {
        return createActionWithIcon(
                DevPilotMessageBundle.get("devpilot.action.edit.settings"),
                ThemeUtils.isDarkTheme() ? DevPilotIcons.SETTINGS_DARK : DevPilotIcons.SETTINGS,
                event -> ShowSettingsUtil.getInstance().showSettingsDialog(event.getProject(), DevPilotSettingsConfigurable.class),
                true
        );
    }

    private static DumbAwareAction createAccountShowAction() {
        String account = DevPilotMessageBundle.get("devpilot.status.account");
        String userName = LoginUtils.getUsername();
        return createActionWithIcon(
                account + userName,
                ThemeUtils.isDarkTheme() ? DevPilotIcons.ACCOUNT_DARK : DevPilotIcons.ACCOUNT,
                null, false);
    }

    private static DumbAwareAction createActionWithIcon(String text, Icon icon,
                                                        Consumer<? super AnActionEvent> actionPerformed, Boolean enabled) {
        return new DumbAwareAction(text, "", icon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                actionPerformed.consume(e);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(enabled);
            }
        };
    }
}