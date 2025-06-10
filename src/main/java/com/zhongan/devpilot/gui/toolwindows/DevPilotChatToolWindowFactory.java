package com.zhongan.devpilot.gui.toolwindows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.zhongan.devpilot.actions.toolbar.ToolbarFeedbackAction;
import com.zhongan.devpilot.actions.toolbar.ToolbarHistorySessionAction;
import com.zhongan.devpilot.actions.toolbar.ToolbarManualIndexAction;
import com.zhongan.devpilot.actions.toolbar.ToolbarMcpConfigurationAction;
import com.zhongan.devpilot.actions.toolbar.ToolbarNewSessionAction;
import com.zhongan.devpilot.actions.toolbar.ToolbarUserProfileAction;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;

public class DevPilotChatToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var devPilotChatToolWindowService = project.getService(DevPilotChatToolWindowService.class);
        var contentFactory = getContentFactory();
        var webPanel = new JPanel(new BorderLayout());

        var devPilotChatToolWindow = devPilotChatToolWindowService.getDevPilotChatToolWindow();

        if (devPilotChatToolWindow != null && devPilotChatToolWindow.getDevPilotChatToolWindowPanel() != null) {
            webPanel.add(devPilotChatToolWindow.getDevPilotChatToolWindowPanel());
            Content content = contentFactory.createContent(webPanel, "", false);
            toolWindow.getContentManager().addContent(content);
            toolWindow.setTitleActions(List.of(new ToolbarMcpConfigurationAction(), new ToolbarNewSessionAction(),new ToolbarHistorySessionAction(),new ToolbarFeedbackAction(), new ToolbarUserProfileAction(), new ToolbarManualIndexAction()));
        }
    }

    private ContentFactory getContentFactory() {
        try {
            Method getInstanceMethod = ContentFactory.class.getMethod("getInstance");
            return (ContentFactory) getInstanceMethod.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            try {
                Class<?> serviceClass = Class.forName("com.intellij.ui.content.ContentFactory$SERVICE");
                Method getInstanceMethod = serviceClass.getMethod("getInstance");
                return (ContentFactory) getInstanceMethod.invoke(null);
            } catch (Exception ex) {
                throw new RuntimeException("无法获取 ContentFactory 实例", ex);
            }
        }
    }
}
