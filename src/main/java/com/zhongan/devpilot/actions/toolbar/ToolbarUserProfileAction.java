package com.zhongan.devpilot.actions.toolbar;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.LoginUtils;

import org.jetbrains.annotations.NotNull;

import static com.zhongan.devpilot.constant.DefaultConst.PROFILE_URL;

public class ToolbarUserProfileAction extends AnAction {
    public ToolbarUserProfileAction() {
        super(DevPilotMessageBundle.get("devpilot.toolbarUserProfileAction.text"),
                DevPilotMessageBundle.get("devpilot.toolbarUserProfileAction.text"),
                DevPilotIcons.USER_PROFILE);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse(LoginUtils.buildAuthUrl(PROFILE_URL));
    }
}
