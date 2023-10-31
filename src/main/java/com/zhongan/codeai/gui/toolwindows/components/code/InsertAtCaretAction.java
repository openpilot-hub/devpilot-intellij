package com.zhongan.codeai.gui.toolwindows.components.code;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;

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
        Editor textEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (textEditor == null) {
            return;
        }
        int offset = textEditor.getCaretModel().getOffset();
        String generatedText = this.editor.getSelectionModel().hasSelection() ?
                this.editor.getSelectionModel().getSelectedText() : this.editor.getDocument().getText();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            textEditor.getDocument().insertString(offset, generatedText);

            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(textEditor.getDocument());
            if (psiFile != null) {
                PsiDocumentManager.getInstance(project).commitDocument(textEditor.getDocument());
                CodeStyleManager.getInstance(project).reformatText(psiFile, offset, offset + generatedText.length());
            }
        });
    }

}
