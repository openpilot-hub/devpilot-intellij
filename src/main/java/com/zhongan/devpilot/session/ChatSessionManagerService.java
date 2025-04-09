package com.zhongan.devpilot.session;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service
public final class ChatSessionManagerService {
    private final ChatSessionManager sessionManager;
    
    public ChatSessionManagerService(Project project) {
        this.sessionManager = new ChatSessionManager(project);
    }
    
    public ChatSessionManager getSessionManager() {
        return sessionManager;
    }
}