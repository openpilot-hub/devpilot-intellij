package com.zhongan.codeai.gui.toolwindows.components.code;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InsertAtCaretAction implements ActionListener {

    private final Editor editor;

    private final Project project;

    public InsertAtCaretAction(Editor editor, Project project) {
        this.editor = editor;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }
        int offset = editor.getCaretModel().getOffset();
        String text = this.editor.getDocument().getText();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().insertString(offset, text);
        });
    }

}
