package com.zhongan.devpilot.sse.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhongan.devpilot.session.model.ChatSession;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PartialMessage {

    private String clientId;

    private String sessionId;

    private String thought;

    private String actionType;

    private Object action;

    private boolean completed;

    private boolean thoughtCompleted;

    private ChatSession session;

    private String result;

    private String serverName;

    private String componentType;

    private String componentName;

    private String responseId;

    private int statusCode;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getThought() {
        return thought;
    }

    public void setThought(String thought) {
        this.thought = thought;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Object getAction() {
        return action;
    }

    public void setAction(Object action) {
        this.action = action;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isThoughtCompleted() {
        return thoughtCompleted;
    }

    public void setThoughtCompleted(boolean thoughtCompleted) {
        this.thoughtCompleted = thoughtCompleted;
    }

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
