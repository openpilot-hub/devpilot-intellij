package com.zhongan.devpilot.gui.toolwindows.components;

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

    private String fileUrl;

    private String filePresentableUrl;

    private Integer selectedStartLine;

    private Integer selectedEndLine;

    public EditorInfo(Editor chosenEditor) {
        this.chosenEditor = chosenEditor;

        VirtualFile file = FileDocumentManager.getInstance().getFile(chosenEditor.getDocument());
        if (file != null) {
            this.fileUrl = file.getUrl();
            this.filePresentableUrl = file.getPresentableUrl();
            this.fileName = file.getName();
            LocalFilePath localFilePath = new LocalFilePath(file.getPath(), false);
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

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFilePresentableUrl() {
        return filePresentableUrl;
    }

    public void setFilePresentableUrl(String filePresentableUrl) {
        this.filePresentableUrl = filePresentableUrl;
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
