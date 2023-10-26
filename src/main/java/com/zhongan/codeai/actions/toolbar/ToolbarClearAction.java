package com.zhongan.codeai.actions.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.zhongan.codeai.CodeAIIcons;
import com.zhongan.codeai.gui.toolwindows.CodeAIChatToolWindowFactory;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import org.jetbrains.annotations.NotNull;

public class ToolbarClearAction extends AnAction {
    public ToolbarClearAction() {
        super(CodeAIMessageBundle.get("codeai.toolbarClearAction.text"),
                CodeAIMessageBundle.get("codeai.toolbarClearAction.text"),
                CodeAIIcons.CLEAR_ICON);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CodeAIChatToolWindowFactory.getCodeAIChatToolWindow(e.getProject()).clearSession();
    }
}
