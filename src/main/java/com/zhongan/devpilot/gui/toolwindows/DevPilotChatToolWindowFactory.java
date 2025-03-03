package com.zhongan.devpilot.gui.toolwindows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.zhongan.devpilot.actions.toolbar.ToolbarFeedbackAction;
import com.zhongan.devpilot.actions.toolbar.ToolbarManualIndexAction;
import com.zhongan.devpilot.actions.toolbar.ToolbarUserProfileAction;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;

public class DevPilotChatToolWindowFactory implements ToolWindowFactory {
    static {
        JBCefApp.getInstance();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var devPilotChatToolWindowService = project.getService(DevPilotChatToolWindowService.class);
        var contentFactory = ContentFactory.SERVICE.getInstance();
        var webPanel = new JPanel(new BorderLayout());

        var devPilotChatToolWindow = devPilotChatToolWindowService.getDevPilotChatToolWindow();

        if (devPilotChatToolWindow != null && devPilotChatToolWindow.getDevPilotChatToolWindowPanel() != null) {
            webPanel.add(devPilotChatToolWindow.getDevPilotChatToolWindowPanel());
            Content content = contentFactory.createContent(webPanel, "", false);
            toolWindow.getContentManager().addContent(content);
            toolWindow.setTitleActions(List.of(new ToolbarFeedbackAction(), new ToolbarUserProfileAction(), new ToolbarManualIndexAction()));
        }
    }
}
