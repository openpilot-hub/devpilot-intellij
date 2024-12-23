package com.zhongan.devpilot.embedding.entity.java.function;

import com.zhongan.devpilot.embedding.entity.DevPilotFileInfo;
import com.zhongan.devpilot.embedding.entity.FunctionMeta;
import com.zhongan.devpilot.embedding.entity.java.anno.AnnotationMeta;

import java.util.List;

public class JavaFunctionMeta extends FunctionMeta {

    private List<AnnotationMeta> annotations;

    private String name;

    private String signature;

    private DevPilotFileInfo returnType;

    private List<DevPilotFileInfo> arguments;

    public List<AnnotationMeta> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationMeta> annotations) {
        this.annotations = annotations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public DevPilotFileInfo getReturnType() {
        return returnType;
    }

    public void setReturnType(DevPilotFileInfo returnType) {
        this.returnType = returnType;
    }

    public List<DevPilotFileInfo> getArguments() {
        return arguments;
    }

    public void setArguments(List<DevPilotFileInfo> arguments) {
        this.arguments = arguments;
    }

}
