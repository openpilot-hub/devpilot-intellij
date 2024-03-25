package com.zhongan.devpilot.listener;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.util.GitUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.zhongan.devpilot.util.LoginUtils;
import org.jetbrains.annotations.NotNull;

import static com.zhongan.devpilot.util.GitUtil.isRepoEmbedded;

public class DevPilotFileEditorListener implements FileEditorManagerListener {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void registerListener() {
        ApplicationManager.getApplication()
                .getMessageBus().connect().subscribe(FILE_EDITOR_MANAGER, new DevPilotFileEditorListener());
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        if (!LoginUtils.isLogin() || LoginUtils.getLoginType().equals("wx")) {
            return;
        }

        VirtualFile file = event.getNewEditor() != null ? event.getNewEditor().getFile() : null;
        if (file == null) {
            return;
        }
        executorService.execute(() -> handleSelectionChanged(file, event.getManager().getProject()));
    }

    private void handleSelectionChanged(VirtualFile file, Project project) {
        DevPilotChatToolWindowService service = project.getService(DevPilotChatToolWindowService.class);

        String repoName = GitUtil.getRepoNameFromFile(project, file);
        if (repoName == null) {
            service.presentRepoCodeEmbeddedState(false, null);
            return;
        }

        Boolean repoEmbedded = isRepoEmbedded(repoName);
        service.presentRepoCodeEmbeddedState(repoEmbedded, repoName);
    }
}
