package com.zhongan.devpilot.statusBar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DevPilotStatusBarWidgetFactory extends StatusBarEditorBasedWidgetFactory {
    public DevPilotStatusBarWidgetFactory() {

    }

    @Override
    public @NonNls @NotNull String getId() {
        return "com.zhongan.devpilot.status.widget";
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "DevPilot";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new DevPilotStatusBarBaseWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {

    }
}
