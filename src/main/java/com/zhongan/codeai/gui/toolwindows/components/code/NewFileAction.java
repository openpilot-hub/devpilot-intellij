package com.zhongan.codeai.gui.toolwindows.components.code;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NewFileAction implements ActionListener {

    private final Editor editor;

    private final String fileExtension;

    private final Project project;

    public NewFileAction(Editor editor, String fileExtension, Project project) {
        this.editor = editor;
        this.fileExtension = fileExtension;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
        PsiFile psiFile = psiFileFactory.createFileFromText("test." + fileExtension, editor.getDocument().getText());
        EditorHelper.openInEditor(psiFile);
    }
}
