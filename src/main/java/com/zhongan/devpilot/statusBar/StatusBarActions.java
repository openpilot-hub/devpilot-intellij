//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.statusBar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import java.util.ArrayList;
import java.util.List;

public class StatusBarActions {
    public StatusBarActions() {
    }

    public static DefaultActionGroup buildStatusBarActionsGroup(Project project) {
        List<AnAction> actions = new ArrayList();
        actions.add(createCompletionsAction());
        return new DefaultActionGroup(actions);
    }

    private static DumbAwareAction createCompletionsAction() {
        return CompletionSettingsState.getInstance().getEnable() ? DumbAwareAction.create(DevPilotMessageBundle.get("devpilot.settings.service.code.completion.disabled.desc"), (event) -> {
            CompletionSettingsState.getInstance().setEnable(false);
        }) : DumbAwareAction.create(DevPilotMessageBundle.get("devpilot.settings.service.code.completion.enable.desc"), (event) -> {
            CompletionSettingsState.getInstance().setEnable(true);
        });
    }
}
