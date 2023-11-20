package com.zhongan.devpilot.actions.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.gui.toolwindows.DevPilotChatToolWindowFactory;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import org.jetbrains.annotations.NotNull;

public class ToolbarClearAction extends AnAction {
    public ToolbarClearAction() {
        super(DevPilotMessageBundle.get("devpilot.toolbarClearAction.text"),
                DevPilotMessageBundle.get("devpilot.toolbarClearAction.text"),
                DevPilotIcons.CLEAR_ICON);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DevPilotChatToolWindowFactory.getDevPilotChatToolWindow(e.getProject()).clearSession();
    }
}
