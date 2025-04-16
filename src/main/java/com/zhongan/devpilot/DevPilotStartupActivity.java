package com.zhongan.devpilot;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.zhongan.devpilot.actions.editor.popupmenu.PopupMenuEditorActionGroupUtil;
import com.zhongan.devpilot.embedding.LocalEmbeddingService;
import com.zhongan.devpilot.listener.DevPilotLineIconListener;
import com.zhongan.devpilot.network.DevPilotAvailabilityChecker;
import com.zhongan.devpilot.update.DevPilotUpdate;

import org.jetbrains.annotations.NotNull;

public class DevPilotStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        PopupMenuEditorActionGroupUtil.refreshActions(project);

        new DevPilotUpdate.DevPilotUpdateTask(project).queue();
        new DevPilotAvailabilityChecker(project).checkNetworkAndLogStatus();
        EditorFactory.getInstance().getEventMulticaster().addCaretListener(new DevPilotLineIconListener(project), project);

        LocalEmbeddingService.start(project);
    }

}
