package com.zhongan.devpilot.gui.toolwindows;

import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.zhongan.devpilot.actions.toolbar.ToolbarClearAction;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindow;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class DevPilotChatToolWindowFactory implements ToolWindowFactory {

    private static final Map<Project, DevPilotChatToolWindow> devPilotChatToolWindows = Maps.newConcurrentMap();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DevPilotChatToolWindow devPilotChatToolWindow = new DevPilotChatToolWindow(project, toolWindow);
        devPilotChatToolWindows.put(project, devPilotChatToolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content content = contentFactory.createContent(devPilotChatToolWindow.getDevPilotChatToolWindowPanel(), "", false);

        toolWindow.getContentManager().addContent(content);

        toolWindow.setTitleActions(List.of(new ToolbarClearAction()));
    }

    public static DevPilotChatToolWindow getDevPilotChatToolWindow(@NotNull Project project) {
        return devPilotChatToolWindows.get(project);
    }
}
