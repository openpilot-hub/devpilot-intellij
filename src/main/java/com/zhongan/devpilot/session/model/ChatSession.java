package com.zhongan.devpilot.session.model;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {
    private String id;

    private long createTime;

    private long updateTime;

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

    public long getCreateTime() {
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
}
