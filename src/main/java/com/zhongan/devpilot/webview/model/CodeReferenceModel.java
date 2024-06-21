package com.zhongan.devpilot.webview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhongan.devpilot.enums.EditorActionEnum;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeReferenceModel {
    private String fileUrl;

    private String fileName;

    private Integer selectedStartLine;

    private Integer selectedEndLine;

    @JsonIgnore
    private EditorActionEnum type;

    public CodeReferenceModel() {

    }

    public CodeReferenceModel(String fileUrl, String fileName, Integer selectedStartLine, Integer selectedEndLine, EditorActionEnum type) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.selectedStartLine = selectedStartLine;
        this.selectedEndLine = selectedEndLine;
        this.type = type;
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

    public EditorActionEnum getType() {
        return type;
    }

    public void setType(EditorActionEnum type) {
        this.type = type;
    }
}
