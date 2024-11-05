package com.zhongan.devpilot.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.agents.DevPilotAgentsRunner;

import org.jetbrains.annotations.NotNull;

public class MultiProjectManagerListener implements ProjectManagerListener {

    private static final DevPilotAgentsRunner AGENTS_RUNNER = new DevPilotAgentsRunner();

    public MultiProjectManagerListener() {
    }

    public void projectOpened(@NotNull Project project) {
        try {
            // TODO:: 判断系统是否兼容，不兼容场景直接跳过这个功能
            AGENTS_RUNNER.run();
        } catch (Exception e) {
            DevPilotNotification.warn("Error occurred while running agents." + e.getMessage());
        }
    }

    public void projectClosing(@NotNull Project project) {
        AGENTS_RUNNER.findProcessAndKill();
    }
}