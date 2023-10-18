package com.zhongan.codeai.gui.toolwindows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.zhongan.codeai.gui.toolwindows.chat.CodeAIChatToolWindow;

import org.jetbrains.annotations.NotNull;

public class CodeAIChatToolWindowFactory implements ToolWindowFactory {
    public static CodeAIChatToolWindow codeAIChatToolWindow = null;
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        codeAIChatToolWindow = new CodeAIChatToolWindow(project, toolWindow);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content content = contentFactory.createContent(codeAIChatToolWindow.getCodeAIChatToolWindowPanel(), "", false);

        toolWindow.getContentManager().addContent(content);
    }
}
