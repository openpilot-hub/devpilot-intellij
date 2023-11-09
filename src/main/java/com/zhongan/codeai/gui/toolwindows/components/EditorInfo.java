package com.zhongan.codeai.gui.toolwindows.components;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;

import javax.swing.Icon;

public class EditorInfo {

    private Editor chosenEditor;

    private Icon fileIcon;

    private String fileName;

    private String fileLocalPath;

    private Integer selectedStartLine;

    private Integer selectedEndLine;

    public EditorInfo(Editor chosenEditor) {
        this.chosenEditor = chosenEditor;

        VirtualFile file = FileDocumentManager.getInstance().getFile(chosenEditor.getDocument());
        if (file != null) {
            this.fileLocalPath = file.getPath();
            this.fileName = file.getName();
            LocalFilePath localFilePath = new LocalFilePath(this.fileLocalPath, false);
            this.fileIcon = VcsUtil.getIcon(chosenEditor.getProject(), localFilePath);
        }

        SelectionModel selectionModel = chosenEditor.getSelectionModel();
        this.selectedStartLine = chosenEditor.getDocument().getLineNumber(selectionModel.getSelectionStart()) + 1;
        this.selectedEndLine = chosenEditor.getDocument().getLineNumber(selectionModel.getSelectionEnd()) + 1;
    }

    public Editor getChosenEditor() {
        return chosenEditor;
    }

    public void setChosenEditor(Editor chosenEditor) {
        this.chosenEditor = chosenEditor;
    }

    public Icon getFileIcon() {
        return fileIcon;
    }

    public void setFileIcon(Icon fileIcon) {
        this.fileIcon = fileIcon;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileLocalPath() {
        return fileLocalPath;
    }

    public void setFileLocalPath(String fileLocalPath) {
        this.fileLocalPath = fileLocalPath;
    }

    public Integer getSelectedStartLine() {
        return selectedStartLine;
    }

    public void setSelectedStartLine(Integer selectedStartLine) {
        this.selectedStartLine = selectedStartLine;
    }

    public Integer getSelectedEndLine() {
        return selectedEndLine;
    }

    public void setSelectedEndLine(Integer selectedEndLine) {
        this.selectedEndLine = selectedEndLine;
    }
}
