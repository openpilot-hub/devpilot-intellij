package com.zhongan.devpilot.integrations.llms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

public class DevPilotMessage {


    @JsonIgnore
    private String id;

    // user, assistant, system
    private String role;


    /**
     * 提示词类型
     * PERFORMANCE_CHECK
     * GENERATE_COMMENTS
     * GENERATE_METHOD_COMMENTS
     * ......
     */
    private String commandType;


    /**
     *  提示词元数据
     *  selectedCode -> "your code"
     *  language -> "java"
     *  mockFramework -> "junit"
     *  ......
     */
    private Map<String, String> promptData;

    /**
     * 用户输入聊天对话信息
     */
    private String content;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Map<String, String> getPromptData() {
        return promptData;
    }

    public void setPromptData(Map<String, String> promptData) {
        this.promptData = promptData;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

}
