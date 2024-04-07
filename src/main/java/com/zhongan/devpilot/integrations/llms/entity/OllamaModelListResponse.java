package com.zhongan.devpilot.integrations.llms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaModelListResponse {
    private List<Model> models;

    public List<Model> getModels() {
        return models;
    }

    public void setModels(List<Model> models) {
        this.models = models;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Model {

        private String name;

        private long size;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

}
