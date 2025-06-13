package com.zhongan.devpilot.listener;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.mcp.McpConfigurationHandler;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class McpConfigurationFileChangeListener implements Disposable, ProjectManagerListener {
    private final MessageBusConnection connection;

    public McpConfigurationFileChangeListener() {
        connection = ApplicationManager.getApplication().getMessageBus().connect(this);
    }

    @Override
    public void projectOpened(@NotNull Project project) {
        registerConfigFileListener();
    }

    private void registerConfigFileListener() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    String path = event.getPath();
                    File configFile = new File(BinaryManager.INSTANCE.getHomeDir(), "mcp_configuration.json");
                    if (configFile.exists() && path.equals(configFile.getAbsolutePath())) {
                        long lastModified = configFile.lastModified();
                        McpConfigurationHandler.INSTANCE.handleConfigFileChanged(path, lastModified);
                    }
                }
            }
        });
    }
    
    @Override
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
        }
    }

    public static McpConfigurationFileChangeListener getInstance() {
        return ApplicationManager.getApplication().getService(McpConfigurationFileChangeListener.class);
    }
}
