package com.zhongan.codeai.gui.toolwindows;

import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.zhongan.codeai.actions.toolbar.ToolbarClearAction;
import com.zhongan.codeai.gui.toolwindows.chat.CodeAIChatToolWindow;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class CodeAIChatToolWindowFactory implements ToolWindowFactory {

    private static final Map<Project, CodeAIChatToolWindow> codeAIChatToolWindows = Maps.newConcurrentMap();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CodeAIChatToolWindow codeAIChatToolWindow = new CodeAIChatToolWindow(project, toolWindow);
        codeAIChatToolWindows.put(project, codeAIChatToolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content content = contentFactory.createContent(codeAIChatToolWindow.getCodeAIChatToolWindowPanel(), "", false);

        toolWindow.getContentManager().addContent(content);

        toolWindow.setTitleActions(List.of(new ToolbarClearAction()));
    }

    public static CodeAIChatToolWindow getCodeAIChatToolWindow(@NotNull Project project) {
        return codeAIChatToolWindows.get(project);
    }
}
