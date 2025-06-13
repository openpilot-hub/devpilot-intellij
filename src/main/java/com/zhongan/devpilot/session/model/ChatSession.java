package com.zhongan.devpilot.session.model;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {
    private String id;

    private long createTime;

    private long updateTime;

    private int chatMode;

    private boolean abort;

    private boolean containsRequireToolPrompts;

    private boolean containsRequireResourcePrompts;

    private boolean containsRequirePromptsPrompts;

    private List<MessageModel> historyMessageList;

    private List<DevPilotMessage> historyRequestMessageList;

    public ChatSession() {
        this.historyMessageList = new ArrayList<>();
        this.historyRequestMessageList = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public List<MessageModel> getHistoryMessageList() {
        return historyMessageList;
    }

    public void setHistoryMessageList(List<MessageModel> historyMessageList) {
        this.historyMessageList = historyMessageList;
    }

    public List<DevPilotMessage> getHistoryRequestMessageList() {
        return historyRequestMessageList;
    }

    public void setHistoryRequestMessageList(List<DevPilotMessage> historyRequestMessageList) {
        this.historyRequestMessageList = historyRequestMessageList;
    }

    public int getChatMode() {
        return chatMode;
    }

    public void setChatMode(int chatMode) {
        this.chatMode = chatMode;
    }

    public boolean isAbort() {
        return abort;
    }

    public void setAbort(boolean abort) {
        this.abort = abort;
    }

    public boolean isContainsRequireToolPrompts() {
        return containsRequireToolPrompts;
    }

    public void setContainsRequireToolPrompts(boolean containsRequireToolPrompts) {
        this.containsRequireToolPrompts = containsRequireToolPrompts;
    }

    public boolean isContainsRequireResourcePrompts() {
        return containsRequireResourcePrompts;
    }

    public void setContainsRequireResourcePrompts(boolean containsRequireResourcePrompts) {
        this.containsRequireResourcePrompts = containsRequireResourcePrompts;
    }

    public boolean isContainsRequirePromptsPrompts() {
        return containsRequirePromptsPrompts;
    }

    public void setContainsRequirePromptsPrompts(boolean containsRequirePromptsPrompts) {
        this.containsRequirePromptsPrompts = containsRequirePromptsPrompts;
    }
}
