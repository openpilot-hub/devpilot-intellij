package com.zhongan.devpilot.embedding.entity.request;

import com.zhongan.devpilot.embedding.entity.DevPilotFileInfo;
import com.zhongan.devpilot.embedding.entity.FunctionMeta;
import com.zhongan.devpilot.embedding.entity.java.file.JavaFileMeta;

import java.util.Date;
import java.util.UUID;

public class VectorIndexRequest {

    private String recordId;

    private String filePath;

    private String fileName;

    private String fileType;

    private int startOffset;

    private int endOffset;

    private int startLine;

    private int endLine;

    private int startColumn;

    private int endColumn;

    private String fileHash;

    private String chunkHash;

    private Date timestamp;

    private String comments;

    private String code;

    private String codeType;

    private String llmSummary;

    private double[] llmSummaryVector;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
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

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public void setChunkHash(String chunkHash) {
        this.chunkHash = chunkHash;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeType() {
        return codeType;
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    public String getLlmSummary() {
        return llmSummary;
    }

    public void setLlmSummary(String llmSummary) {
        this.llmSummary = llmSummary;
    }

    public double[] getLlmSummaryVector() {
        return llmSummaryVector;
    }

    public void setLlmSummaryVector(double[] llmSummaryVector) {
        this.llmSummaryVector = llmSummaryVector;
    }

    public static VectorIndexRequest from(DevPilotFileInfo fileInfo, FunctionMeta functionMeta) {
        VectorIndexRequest request = new VectorIndexRequest();
        request.setRecordId(UUID.randomUUID().toString());
        request.setFilePath(fileInfo.getFilePath());
        request.setFileName(fileInfo.getFileName());
        request.setStartOffset(functionMeta.getFunctionStartOffset());
        request.setEndOffset(functionMeta.getFunctionEndOffset());
        request.setStartLine(functionMeta.getStartLine());
        request.setEndLine(functionMeta.getEndLine());
        request.setStartColumn(functionMeta.getStartColumn());
        request.setEndColumn(functionMeta.getEndColumn());
        request.setFileHash(fileInfo.getFileHash());
        request.setCode(functionMeta.getContent());
        request.setComments(functionMeta.getComments());
        request.setChunkHash(functionMeta.getChunkHash());
        request.setFileType(fileInfo.getFileMeta().getFileType());
        request.setCodeType("functionDef");
        request.setTimestamp(new Date());
        return request;
    }

    public static VectorIndexRequest from(DevPilotFileInfo fileInfo, JavaFileMeta javaFileMeta) {
        VectorIndexRequest request = new VectorIndexRequest();
        request.setRecordId(UUID.randomUUID().toString());
        request.setFilePath(fileInfo.getFilePath());
        request.setFileName(fileInfo.getFileName());
        request.setStartOffset(javaFileMeta.getStartOffset());
        request.setEndOffset(javaFileMeta.getEndOffset());
        request.setFileHash(fileInfo.getFileHash());
        request.setStartLine(javaFileMeta.getStartLine());
        request.setEndLine(javaFileMeta.getEndLine());
        request.setStartColumn(javaFileMeta.getStartColumn());
        request.setEndColumn(javaFileMeta.getEndColumn());
        request.setChunkHash(javaFileMeta.getChunkHash());
        request.setFileType(javaFileMeta.getFileType());
        request.setCode(javaFileMeta.getClazzDef());
        request.setCodeType("classDef");
        request.setComments(javaFileMeta.getComments());
        request.setTimestamp(new Date());
        return request;
    }
}
