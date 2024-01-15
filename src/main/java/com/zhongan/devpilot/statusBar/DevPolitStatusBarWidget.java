package com.zhongan.devpilot.statusBar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.util.Consumer;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class DevPolitStatusBarWidget extends EditorBasedWidget
        implements StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {
    private static final String EMPTY_SYMBOL = "\u0000";
    private volatile boolean isLimited = false;

    //TODO 从登录文件中获取
    private volatile Boolean isLoggedIn = true;

    @Nullable
    private volatile Boolean isForcedRegistration = null;

    public DevPolitStatusBarWidget(@NotNull Project project) {
        super(project);
        //todo 初始化setting
    }

    public Icon getIcon() {
        Icon icon = DevPilotIcons.SYSTEM_ICON;
        if (!CompletionSettingsState.getInstance().getEnable()) {
            return IconLoader.getTransparentIcon(icon, 0.3f);
        }
        return icon;
    }

    public @Nullable("null means the widget is unable to show the popup") ListPopup getPopupStep() {
        return createPopup();
    }

    public String getSelectedValue() {
        //todo 登录图标
        return EMPTY_SYMBOL;
    }

    @Nullable
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Nullable
    public WidgetPresentation getPresentation(@NotNull PlatformType type) {
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
                                null,//todo action group创建

                                DataManager.getInstance()
                                        .getDataContext(myStatusBar != null ? myStatusBar.getComponent() : null),
                                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                true)
                ;
        //todo action 监听
        popup.addListener(null);
        return popup;
    }

    @Nullable
    public String getTooltipText() {
        return "DevPilot (Click to open settings)";
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