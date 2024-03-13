//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
    public StatusBarProvider() {
    }

    public @NotNull String getId() {
        String var10000 = this.getClass().getName();

        return var10000;
    }

    public @Nls @NotNull String getDisplayName() {
        return "DevPilot";
    }

    public boolean isAvailable(@NotNull Project project) {

        return true;
    }

    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {


        Logger.getInstance(this.getClass()).info("creating status bar widget");
        return new DevPilotStatusBarWidget(project);
    }

    public void disposeWidget(@NotNull StatusBarWidget widget) {


        Logger.getInstance(this.getClass()).info("disposing status bar widget");
        Disposer.dispose(widget);
    }

    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {


        return true;
    }
}
