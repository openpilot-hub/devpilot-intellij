package com.zhongan.devpilot.embedding.entity.java.file;

import com.zhongan.devpilot.embedding.entity.DevPilotFileInfo;
import com.zhongan.devpilot.embedding.entity.FileMeta;
import com.zhongan.devpilot.embedding.entity.java.anno.AnnotationMeta;

import java.util.List;

public class JavaFileMeta extends FileMeta {

    private String packageName;

    private String imports;

    private String classDeclaration;

    private String typeName;

    private String typeFullName;

    private List<DevPilotFileInfo> fields;

    private List<AnnotationMeta> annotationMetas;

    private String comments;

    private String llmSummary;

    private String clazzDef;

    private String chunkHash;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getImports() {
        return imports;
    }

    public void setImports(String imports) {
        this.imports = imports;
    }

    public String getClassDeclaration() {
        return classDeclaration;
    }

    public void setClassDeclaration(String classDeclaration) {
        this.classDeclaration = classDeclaration;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeFullName() {
        return typeFullName;
    }

    public void setTypeFullName(String typeFullName) {
        this.typeFullName = typeFullName;
    }

    public List<DevPilotFileInfo> getFields() {
        return fields;
    }

    public void setFields(List<DevPilotFileInfo> fields) {
        this.fields = fields;
    }

    public List<AnnotationMeta> getAnnotationMetas() {
        return annotationMetas;
    }

    public void setAnnotationMetas(List<AnnotationMeta> annotationMetas) {
        this.annotationMetas = annotationMetas;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getLlmSummary() {
        return llmSummary;
    }

    public void setLlmSummary(String llmSummary) {
        this.llmSummary = llmSummary;
    }

    public String getClazzDef() {
        return clazzDef;
    }

    public void setClazzDef(String clazzDef) {
        this.clazzDef = clazzDef;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public void setChunkHash(String chunkHash) {
        this.chunkHash = chunkHash;
    }
}
