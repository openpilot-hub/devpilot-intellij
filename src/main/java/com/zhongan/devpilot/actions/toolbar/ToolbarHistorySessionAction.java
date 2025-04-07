package com.zhongan.devpilot.actions.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import org.jetbrains.annotations.NotNull;

public class ToolbarHistorySessionAction extends AnAction {
    public ToolbarHistorySessionAction() {
        super(DevPilotMessageBundle.get("devpilot.toolbarHistorySessionAction.text"),
                DevPilotMessageBundle.get("devpilot.toolbarHistorySessionAction.text"),
                AllIcons.Vcs.History);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevPilot");
        if (toolWindow == null) {
            return;
        }
        toolWindow.show();

        var service = project.getService(DevPilotChatToolWindowService.class);
        service.renderHistorySession();
    }
}