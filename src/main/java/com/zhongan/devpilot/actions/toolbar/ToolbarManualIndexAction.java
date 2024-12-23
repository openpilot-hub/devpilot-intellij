package com.zhongan.devpilot.actions.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.embedding.LocalEmbeddingService;
import com.zhongan.devpilot.settings.state.LocalRagSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import org.jetbrains.annotations.NotNull;

public class ToolbarManualIndexAction extends AnAction {
    public ToolbarManualIndexAction() {
        super(DevPilotMessageBundle.get("devpilot.toolbarManualIndexAction.text"),
                DevPilotMessageBundle.get("devpilot.toolbarManualIndexAction.text"),
                DevPilotIcons.MANUAL_INDEX);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var enabled = LocalRagSettingsState.getInstance().getEnable();
        var project = e.getProject();

        if (!enabled) {
            DevPilotNotification.infoAndSetting(project,
                    DevPilotMessageBundle.get("devpilot.notification.index.setting"),
                    DevPilotMessageBundle.get("devpilot.notification.index.message"));
            return;
        }

        DevPilotNotification.info(DevPilotMessageBundle.get("devpilot.notification.index.start.message"));
        LocalEmbeddingService.immediateStart(project);
    }
}
