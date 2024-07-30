package com.zhongan.devpilot.actions.editor.popupmenu;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.psi.PsiElement;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BasicEditorAction extends AnAction {

    BasicEditorAction(
        @Nullable @NlsActions.ActionText String text,
        @Nullable @NlsActions.ActionDescription String description,
        @Nullable Icon icon) {
        super(text, description, icon);
        PopupMenuEditorActionGroupUtil.registerOrReplaceAction(this);
    }

    protected abstract void actionPerformed(Project project, Editor editor, String selectedText, PsiElement psiElement);

    public void actionPerformed(@NotNull AnActionEvent event) {
        var project = event.getProject();
        var editor = event.getData(PlatformDataKeys.EDITOR);
        if (editor != null && project != null) {
            actionPerformed(project, editor, editor.getSelectionModel().getSelectedText(), null);
        }
    }

    public void fastAction(Project project, Editor editor, String selectedText, PsiElement psiElement) {
        actionPerformed(project, editor, selectedText, psiElement);
    }

    public void update(AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        boolean menuAllowed = false;
        if (editor != null && project != null) {
            menuAllowed = editor.getSelectionModel().getSelectedText() != null;
        }
        event.getPresentation().setEnabled(menuAllowed);
    }

}
