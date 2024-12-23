package com.zhongan.devpilot.embedding.entity.java.anno;

import java.util.ArrayList;
import java.util.List;

public class AnnotationMeta {

    private String type;

    private List<AnnotationAttributeMeta> attributes = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AnnotationAttributeMeta> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AnnotationAttributeMeta> attributes) {
        this.attributes = attributes;
    }

}
