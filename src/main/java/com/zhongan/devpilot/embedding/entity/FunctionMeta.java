package com.zhongan.devpilot.embedding.entity;

import java.util.List;

public class FunctionMeta {

    private int functionStartOffset;

    private int functionEndOffset;

    private int startLine;

    private int endLine;

    private int startColumn;

    private int endColumn;

    private String comments;

    private String content;

    private String functionLLMSummary;

    private String chunkHash;

    private List<FunctionPartBlockMeta> codeBlocks;

    public int getFunctionStartOffset() {
        return functionStartOffset;
    }

    public void setFunctionStartOffset(int functionStartOffset) {
        this.functionStartOffset = functionStartOffset;
    }

    public int getFunctionEndOffset() {
        return functionEndOffset;
    }

    public void setFunctionEndOffset(int functionEndOffset) {
        this.functionEndOffset = functionEndOffset;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getFunctionLLMSummary() {
        return functionLLMSummary;
    }

    public void setFunctionLLMSummary(String functionLLMSummary) {
        this.functionLLMSummary = functionLLMSummary;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public void setChunkHash(String chunkHash) {
        this.chunkHash = chunkHash;
    }

    public List<FunctionPartBlockMeta> getCodeBlocks() {
        return codeBlocks;
    }

    public void setCodeBlocks(List<FunctionPartBlockMeta> codeBlocks) {
        this.codeBlocks = codeBlocks;
    }

}
