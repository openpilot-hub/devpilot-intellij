package com.zhongan.devpilot.listener;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.zhongan.devpilot.agents.AgentsRunner;
import com.zhongan.devpilot.agents.BinaryManager;

import org.jetbrains.annotations.NotNull;

public class MultiProjectManagerListener implements ProjectManagerListener {

    private static final Logger LOG = Logger.getInstance(MultiProjectManagerListener.class);

    public MultiProjectManagerListener() {
    }

    public void projectOpened(@NotNull Project project) {
        try {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            if (openProjects.length == 1) {
                BinaryManager.INSTANCE.findProcessAndKill();
            }
            AgentsRunner.INSTANCE.run();
        } catch (Exception e) {
            LOG.warn("Error occurred while running agents.", e);
        }
    }

    public void projectClosing(@NotNull Project project) {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 1) {
            BinaryManager.INSTANCE.findProcessAndKill();
        }
    }
}