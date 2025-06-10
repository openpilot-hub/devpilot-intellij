package com.zhongan.devpilot.actions.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

public class ToolbarMcpConfigurationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ToolbarMcpConfigurationAction.class);

    public ToolbarMcpConfigurationAction() {
        super(DevPilotMessageBundle.get("devpilot.toolbarMCPConfigurationAction.text"),
                DevPilotMessageBundle.get("devpilot.toolbarMCPConfigurationAction.text"),
                AllIcons.Vcs.Changelist);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        String configFileName = "mcp_configuration.json";
        File configFile = new File(BinaryManager.INSTANCE.getHomeDir(), configFileName);

        if (!configFile.exists()) {
            try {
                FileWriter writer = new FileWriter(configFile);
                writer.write("{\n  \"mcpServers\": {}\n}");
                writer.close();
            } catch (IOException ex) {
                LOG.warn("Error occurred while creating mcp_configuration.json", ex);
                Messages.showWarningDialog(project, DevPilotMessageBundle.get("devpilot.warning.mcpConfiguration.creation"), DevPilotMessageBundle.get("devpilot.warning"));
                return;
            }
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevPilot");
        if (toolWindow == null) {
            return;
        }
        toolWindow.show();

        var service = project.getService(DevPilotChatToolWindowService.class);
        service.listMcpServers();
    }
}
