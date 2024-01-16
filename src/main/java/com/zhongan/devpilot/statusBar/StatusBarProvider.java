package com.zhongan.devpilot.statusBar;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class StatusBarProvider implements StatusBarWidgetFactory {
    @Override
    public @NotNull String getId() {
        return getClass().getName();
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "DevPolit";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        Logger.getInstance(getClass()).info("creating status bar widget");
        return new DevPolitStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        Logger.getInstance(getClass()).info("disposing status bar widget");
        Disposer.dispose(widget);
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
