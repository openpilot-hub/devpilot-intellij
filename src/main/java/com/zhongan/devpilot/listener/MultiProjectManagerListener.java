package com.zhongan.devpilot.listener;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.zhongan.devpilot.agents.AgentsRunner;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.session.ChatSessionManagerService;
import com.zhongan.devpilot.sse.SSEClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

public class MultiProjectManagerListener implements ProjectManagerListener {

    private static final Logger LOG = Logger.getInstance(MultiProjectManagerListener.class);

    private final ScheduledExecutorService agentStateMonitorScheduler = Executors.newSingleThreadScheduledExecutor();

    private final ScheduledExecutorService agentAutoUpgradeScheduler = Executors.newSingleThreadScheduledExecutor();

    public MultiProjectManagerListener() {
        agentStateMonitorScheduler.scheduleAtFixedRate(
                () -> {
                    boolean ok = BinaryManager.INSTANCE.currentPortAvailable();
                    if (!ok) {
                        if (!BinaryManager.INSTANCE.shouldStartAgent()) {
                            return;
                        }

                        if (!BinaryManager.INSTANCE.reStarting.compareAndSet(false, true) || AgentsRunner.initialRunning.get()) {
                            LOG.info("Agent upgrading, skip monitor.");
                            return;
                        }

                        try {
                            AgentsRunner.INSTANCE.run();
                        } catch (Exception e) {
                            LOG.error("Restart agent failed.", e);
                        } finally {
                            BinaryManager.INSTANCE.reStarting.set(false);
                        }
                    }
                }, 2 * 60, 20, TimeUnit.SECONDS);

        agentAutoUpgradeScheduler.scheduleAtFixedRate(
                () -> {
                    boolean needed = BinaryManager.INSTANCE.checkIfAutoUpgradeNeeded();
                    if (needed) {
                        if (!BinaryManager.INSTANCE.shouldStartAgent()) {
                            return;
                        }
                        if (!BinaryManager.INSTANCE.reStarting.compareAndSet(false, true) || AgentsRunner.initialRunning.get()) {
                            LOG.info("Agent is restarting, skip upgrade.");
                            return;
                        }
                        try {
                            BinaryManager.INSTANCE.upgradeAgent();
                        } catch (Exception e) {
                            LOG.error("Upgrade agent failed.", e);
                        } finally {
                            BinaryManager.INSTANCE.reStarting.set(false);
                        }
                    }
                }, 5 * 60, 2 * 60 * 60, TimeUnit.SECONDS);
    }

    public void projectOpened(@NotNull Project project) {
        try {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            if (openProjects.length == 1) {
                BinaryManager.INSTANCE.findProcessAndKill();
            }
            AgentsRunner.INSTANCE.run();
            LOG.info("Try to connect agent.");
            SSEClient.getInstance(project).connect();
            project.getService(ChatSessionManagerService.class);
        } catch (Exception e) {
            LOG.warn("Error occurred while running agents.", e);
        }
    }

    public void projectClosing(@NotNull Project project) {
        SSEClient.removeInstance(project);
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 1) {
            BinaryManager.INSTANCE.findProcessAndKill();
        }
    }
}