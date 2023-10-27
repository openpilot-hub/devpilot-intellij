package com.zhongan.codeai.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import org.jetbrains.annotations.NotNull;

public class NewChatAction extends AnAction {

    public NewChatAction() {
        super("New Open Pilot Chat", "New chat with Open Pilot", AllIcons.Actions.Find);
        PopupMenuEditorActionGroupUtil.registerOrReplaceAction(this);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var project = event.getProject();
        if (project != null) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Open Pilot");
            if (toolWindow == null) {
                return;
            }
            toolWindow.show();
        }
    }

}
