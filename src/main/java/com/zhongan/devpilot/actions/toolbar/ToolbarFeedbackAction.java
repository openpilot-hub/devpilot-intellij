package com.zhongan.devpilot.actions.toolbar;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.LoginUtils;

import org.jetbrains.annotations.NotNull;

import static com.zhongan.devpilot.constant.DefaultConst.FEEDBACK_URL;

public class ToolbarFeedbackAction extends AnAction {
    public ToolbarFeedbackAction() {
        super(DevPilotMessageBundle.get("devpilot.toolbarFeedbackAction.text"),
                DevPilotMessageBundle.get("devpilot.toolbarFeedbackAction.text"),
                DevPilotIcons.FEEDBACK);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse(LoginUtils.buildAuthUrl(FEEDBACK_URL));
    }
}