package com.zhongan.codeai.actions.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.zhongan.codeai.CodeAIIcons;
import com.zhongan.codeai.gui.toolwindows.CodeAIChatToolWindowFactory;

import org.jetbrains.annotations.NotNull;

public class ToolbarClearAction extends AnAction {
    public ToolbarClearAction() {
        super(CodeAIIcons.CLEAR_ICON);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CodeAIChatToolWindowFactory.getCodeAIChatToolWindow(e.getProject()).clearSession();
    }
}
