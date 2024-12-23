package com.zhongan.devpilot.listener;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.zhongan.devpilot.agents.AgentsRunner;
import com.zhongan.devpilot.agents.BinaryManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

public class MultiProjectManagerListener implements ProjectManagerListener {

    private static final Logger LOG = Logger.getInstance(MultiProjectManagerListener.class);

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public MultiProjectManagerListener() {
        executorService.scheduleAtFixedRate(
                () -> {
                    boolean ok = BinaryManager.INSTANCE.currentPortAvailable();
                    if (!ok) {
                        if (!BinaryManager.INSTANCE.shouldStartAgent()) {
                            return;
                        }
                        try {
                            AgentsRunner.INSTANCE.run();
                        } catch (Exception e) {
                            LOG.error("Restart agent failed.", e);
                        }
                    }
                }, 2 * 60, 20, TimeUnit.SECONDS);
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
        if (openProjects.length == 1 && !ApplicationManager.getApplication().isInternal()) {
            BinaryManager.INSTANCE.findProcessAndKill();
        }
    }
}