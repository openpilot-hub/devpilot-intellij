package com.zhongan.devpilot.webview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhongan.devpilot.enums.EditorActionEnum;

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

    @JsonIgnore
    private EditorActionEnum type;

    public CodeReferenceModel() {

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

    public EditorActionEnum getType() {
        return type;
    }

    public void setType(EditorActionEnum type) {
        this.type = type;
    }
}
