//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.statusBar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.util.Consumer;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.statusBar.CompletionsStateNotifier.Companion;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DevPilotStatusBarWidget extends EditorBasedWidget implements StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {
    private static final String EMPTY_SYMBOL = " ";

    public DevPilotStatusBarWidget(@NotNull Project project) {
        super(project);
        Companion.subscribe((isEnabled) -> {
            this.update();
        });
    }

    public Icon getIcon() {
        return !CompletionSettingsState.getInstance().getEnable() ? DevPilotIcons.SYSTEM_ICON_GRAY : DevPilotIcons.SYSTEM_ICON;
    }

    public @Nullable("null means the widget is unable to show the popup") ListPopup getPopupStep() {
        return this.createPopup();
    }

    public String getSelectedValue() {
        return " ";
    }

    @Nullable
    public StatusBarWidget.@Nullable WidgetPresentation getPresentation() {
        return this;
    }

    public @NotNull String ID() {
        String var10000 = this.getClass().getName();
        return var10000;
    }

    private ListPopup createPopup() {
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup((String)null, StatusBarActions.buildStatusBarActionsGroup(this.myStatusBar != null ? this.myStatusBar.getProject() : null), DataManager.getInstance().getDataContext(this.myStatusBar != null ? this.myStatusBar.getComponent() : null), ActionSelectionAid.SPEEDSEARCH, true);
        return popup;
    }

    public @Nullable String getTooltipText() {
        return "DevPilot";
    }

    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return null;
    }

    private void update() {
        if (this.myStatusBar == null) {
            Logger.getInstance(this.getClass()).warn("Failed to update the status bar");
        } else {
            this.myStatusBar.updateWidget(this.ID());
        }
    }
}
