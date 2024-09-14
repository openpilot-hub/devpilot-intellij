package com.zhongan.devpilot.webview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeReferenceModel {
    private String languageId;

    private String fileUrl;

    private String fileName;

    private String sourceCode;

    private Integer selectedStartLine;

    private Integer selectedStartColumn;

    private Integer selectedEndLine;

    private Integer selectedEndColumn;

    private boolean visible = true;

    @JsonIgnore
    private EditorActionEnum type;

    public CodeReferenceModel() {

    }

    public static CodeReferenceModel getCodeRefFromEditor(EditorInfo editorInfo, EditorActionEnum actionEnum) {
        return new CodeReferenceModel(editorInfo.getLanguageId(), editorInfo.getFilePresentableUrl(),
                editorInfo.getFileName(), editorInfo.getSourceCode(), editorInfo.getSelectedStartLine(),
                editorInfo.getSelectedStartColumn(), editorInfo.getSelectedEndLine(), editorInfo.getSelectedEndColumn(), actionEnum);
    }

    public static List<CodeReferenceModel> getCodeRefListFromPsiElement(Collection<PsiElement> list, EditorActionEnum actionEnum) {
        if (list == null) {
            return null;
        }

        var result = new ArrayList<CodeReferenceModel>();

        for (PsiElement element : list) {
            var ref = getCodeRefFromPsiElement(element, actionEnum);
            if (ref != null) {
                result.add(ref);
            }
        }

        return result;
    }

    public static CodeReferenceModel getCodeRefFromPsiElement(PsiElement element, EditorActionEnum actionEnum) {
        if (element == null) {
            return null;
        }

        var languageId = element.getLanguage().getID();
        var sourceCode = element.getText();

        var psiFile = element.getContainingFile();
        VirtualFile file = null;
        Document document = null;

        if (psiFile != null) {
            file = psiFile.getVirtualFile();
            var project = element.getProject();
            document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        }

        String filePath = null;
        String fileName = null;

        if (file != null) {
            filePath = file.getPath();
            fileName = file.getName();
        }

        Integer startLine = null;
        Integer endLine = null;

        Integer startColumn = null;
        Integer endColumn = null;

        if (document != null) {
            var textRange = element.getTextRange();
            int startOffset = textRange.getStartOffset();
            int endOffset = textRange.getEndOffset();

            startLine = document.getLineNumber(startOffset);
            endLine = document.getLineNumber(endOffset);

            startColumn = startOffset - document.getLineStartOffset(startLine);
            endColumn = endOffset - document.getLineStartOffset(endLine);
        }

        return new CodeReferenceModel(
                languageId, filePath, fileName, sourceCode, startLine, startColumn, endLine, endColumn, actionEnum);
    }

    public CodeReferenceModel(String languageId, String fileUrl, String fileName, String sourceCode,
                              Integer selectedStartLine, Integer selectedStartColumn,
                              Integer selectedEndLine, Integer selectedEndColumn, EditorActionEnum type) {
        this.languageId = languageId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.sourceCode = sourceCode;
        this.selectedStartLine = selectedStartLine;
        this.selectedStartColumn = selectedStartColumn;
        this.selectedEndLine = selectedEndLine;
        this.selectedEndColumn = selectedEndColumn;
        this.type = type;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public EditorActionEnum getType() {
        return type;
    }

    public void setType(EditorActionEnum type) {
        this.type = type;
    }
}
