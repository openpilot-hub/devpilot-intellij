package com.zhongan.devpilot.statusBar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.util.Consumer;
import com.zhongan.devpilot.DevPilotIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class DevPolitStatusBarWidget extends EditorBasedWidget
        implements StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {
    private static final String EMPTY_SYMBOL = "\u0000";

    public DevPolitStatusBarWidget(@NotNull Project project) {
        super(project);
        //todo 初始化setting
    }

    public Icon getIcon() {
        //todo 根据状态区分icon
        return DevPilotIcons.SYSTEM_ICON;
    }

    public @Nullable("null means the widget is unable to show the popup") ListPopup getPopupStep() {
        return createPopup();
    }

    public String getSelectedValue() {
        //todo 区分登录和非登录图标
        return EMPTY_SYMBOL;
    }

    @Nullable
    public WidgetPresentation getPresentation() {
        return this;
    }

    @NotNull
    @Override
    public String ID() {
        return getClass().getName();
    }

    private ListPopup createPopup() {
        ListPopup popup =
                JBPopupFactory.getInstance()
                        .createActionGroupPopup(
                                null,
                                StatusBarActions.buildStatusBarActionsGroup(
                                        myStatusBar != null ? myStatusBar.getProject() : null),
                                DataManager.getInstance()
                                        .getDataContext(myStatusBar != null ? myStatusBar.getComponent() : null),
                                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                true);
        return popup;
    }

    @Nullable
    public String getTooltipText() {
        return "DevPilot";
    }

    @Nullable
    public Consumer<MouseEvent> getClickConsumer() {
        return null;
    }

    private void update() {
        if (myStatusBar == null) {
            Logger.getInstance(getClass()).warn("Failed to update the status bar");
            return;
        }
        myStatusBar.updateWidget(ID());
    }

}