package com.zhongan.devpilot.statusBar;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.zhongan.devpilot.statusBar.status.DevPilotStatusEnum;
import com.zhongan.devpilot.util.LoginUtils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DevPilotStatusBarBaseWidget extends EditorBasedStatusBarPopup {

    private static DevPilotStatusEnum currentStatus = LoginUtils.isLogin() ? DevPilotStatusEnum.LoggedIn : DevPilotStatusEnum.NotLoggedIn;

    public DevPilotStatusBarBaseWidget(@NotNull Project project) {
        super(project, false);
    }

    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        WidgetState widgetState = new WidgetState(currentStatus.getText(), "", true);
        widgetState.setIcon(currentStatus.getIcon());
        return widgetState;
    }

    @Override
    protected @Nullable ListPopup createPopup(DataContext context) {
        return JBPopupFactory.
                getInstance().
                createActionGroupPopup("DevPilot Status",
                        StatusBarActions.buildStatusBarActionsGroup(),
                        context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
    }

    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new DevPilotStatusBarBaseWidget(project);
    }

    @Override
    public @NonNls @NotNull String ID() {
        return "com.zhongan.devpilot.status.widget";
    }

    public static void update(Project project, DevPilotStatusEnum devPilotStatus) {
        currentStatus = devPilotStatus;
        DevPilotStatusBarBaseWidget statusBarWidget = findStatusBarWidget(project);
        if (statusBarWidget != null) {
            statusBarWidget.update(() -> statusBarWidget.myStatusBar.updateWidget("com.zhongan.devpilot.status.widget"));
        }
    }

    private static DevPilotStatusBarBaseWidget findStatusBarWidget(@NotNull Project project) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            StatusBarWidget widget = statusBar.getWidget("com.zhongan.devpilot.status.widget");
            if (widget instanceof DevPilotStatusBarBaseWidget) {
                return (DevPilotStatusBarBaseWidget) widget;
            }
        }

        return null;
    }
}
