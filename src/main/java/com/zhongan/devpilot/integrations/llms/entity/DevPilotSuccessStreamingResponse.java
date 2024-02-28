package com.zhongan.devpilot.integrations.llms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DevPilotSuccessStreamingResponse {

    private String id;

    private String object;

    private Long created;

    private String model;

    private List<Choice> choices;

    private Usage usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {

        private Integer index;

        @JsonProperty("finish_reason")
        private String finishReason;

        private Message delta;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public Message getDelta() {
            return delta;
        }

        public void setDelta(Message delta) {
            this.delta = delta;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {

        @JsonProperty("completion_tokens")
        private String completionTokens;

        @JsonProperty("prompt_tokens")
        private String promptTokens;

        @JsonProperty("total_tokens")
        private String totalTokens;

        public String getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(String completionTokens) {
            this.completionTokens = completionTokens;
        }

        public String getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(String promptTokens) {
            this.promptTokens = promptTokens;
        }

        public String getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(String totalTokens) {
            this.totalTokens = totalTokens;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {

        private String role;

        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }

}
