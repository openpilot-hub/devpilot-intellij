package com.zhongan.devpilot.embedding.entity;

import java.util.List;

public class FileMeta {
    private String fileType;

    private int startOffset;

    private int endOffset;

    private int startLine;

    private int endLine;

    private int startColumn;

    private int endColumn;

    private List<FunctionMeta> functionMetas;

    public String getFileType() {
        return fileType;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public void setStartColumn(int startColumn) {
        this.startColumn = startColumn;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public List<FunctionMeta> getFunctionMetas() {
        return functionMetas;
    }

    public void setFunctionMetas(List<FunctionMeta> functionMetas) {
        this.functionMetas = functionMetas;
    }

}
