package com.zhongan.devpilot.integrations.llms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

public class DevPilotMessage {


    @JsonIgnore
    private String id;

    // user, assistant, system
    private String role;

    // PERFORMANCE_CHECK
    // GENERATE_COMMENTS
    // GENERATE_METHOD_COMMENTS
    // 如果纯聊天对话，没有提示词模版，需要额外设置下类型
    // 如果是类似上面图片中的混合模式，那么需要再结合具体的消息类型判断
    private String commandType;
    
    // selectedCode -> "your code"
    // language -> "java"
    // mockFramework -> "junit"
    // ...
    private Map<String, String> promptData;
    
    // 单纯聊天对话信息
    private String content;

   // eg: 用中文回答
    List<String> additional;

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

    public List<String> getAdditional() {
        return additional;
    }

    public void setAdditional(List<String> additional) {
        this.additional = additional;
    }

}
