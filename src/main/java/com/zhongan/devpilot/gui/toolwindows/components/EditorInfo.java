package com.zhongan.devpilot.gui.toolwindows.components;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import com.zhongan.devpilot.util.LanguageUtil;

import java.util.Locale;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorInfo {

    private Editor chosenEditor;

    private Icon fileIcon;

    private String languageId;

    private String fileName;

    private String fileUrl;

    private String filePresentableUrl;

    private String sourceCode;

    private Integer selectedStartLine;

    private Integer selectedStartColumn;

    private Integer selectedEndLine;

    private Integer selectedEndColumn;

    public EditorInfo(Editor chosenEditor) {
        this.chosenEditor = chosenEditor;

        VirtualFile file = FileDocumentManager.getInstance().getFile(chosenEditor.getDocument());
        if (file != null) {
            this.fileUrl = file.getUrl();
            this.filePresentableUrl = file.getPresentableUrl();
            this.fileName = file.getName();
            LocalFilePath localFilePath = new LocalFilePath(file.getPath(), false);
            this.fileIcon = getIcon(chosenEditor.getProject(), localFilePath);
            var language = LanguageUtil.getLanguageByExtension(file.getExtension());
            if (language != null) {
                this.languageId = language.getLanguageName().toLowerCase(Locale.ROOT);
            }
        }

        SelectionModel selectionModel = chosenEditor.getSelectionModel();
        this.sourceCode = selectionModel.getSelectedText();

        var startPosition = selectionModel.getSelectionStartPosition();
        var endPosition = selectionModel.getSelectionEndPosition();

        if (startPosition != null) {
            var startLogicalPosition = chosenEditor.visualToLogicalPosition(startPosition);

            this.selectedStartLine = startLogicalPosition.line;
            this.selectedStartColumn = startLogicalPosition.column;
        }

        if (endPosition != null) {
            var endLogicalPosition = chosenEditor.visualToLogicalPosition(endPosition);

            this.selectedEndLine = endLogicalPosition.line;
            this.selectedEndColumn = endLogicalPosition.column;
        }
    }

    private Icon getIcon(@Nullable Project project, @NotNull FilePath filePath) {
        if (project != null && project.isDisposed()) return null;
        VirtualFile virtualFile = filePath.getVirtualFile();
        if (virtualFile != null) return IconUtil.getIcon(virtualFile, 0, project);
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(filePath.getName());
        return fileType.getIcon();
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

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
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

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Integer getSelectedStartLine() {
        return selectedStartLine;
    }

    public void setSelectedStartLine(Integer selectedStartLine) {
        this.selectedStartLine = selectedStartLine;
    }

    public Integer getSelectedStartColumn() {
        return selectedStartColumn;
    }

    public void setSelectedStartColumn(Integer selectedStartColumn) {
        this.selectedStartColumn = selectedStartColumn;
    }

    public Integer getSelectedEndLine() {
        return selectedEndLine;
    }

    public void setSelectedEndLine(Integer selectedEndLine) {
        this.selectedEndLine = selectedEndLine;
    }

    public Integer getSelectedEndColumn() {
        return selectedEndColumn;
    }

    public void setSelectedEndColumn(Integer selectedEndColumn) {
        this.selectedEndColumn = selectedEndColumn;
    }
}
