package com.zhongan.devpilot.util;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.ProjectManager;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.LoginTypeEnum;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.statusBar.DevPilotStatusBarBaseWidget;
import com.zhongan.devpilot.statusBar.status.DevPilotStatusEnum;

import java.util.Locale;

import org.jetbrains.ide.BuiltInServerManager;

public class LoginUtils {
    private static final String loginAuthUrl = "https://devpilot-h5.zhongan.com/login?backUrl=%s&source=%s";

    private static final String loginCallback = "http://127.0.0.1:%s/login/auth/callback";

    public static String loginUrl() {
        var callback = String.format(loginCallback, BuiltInServerManager.getInstance().getPort());
        return String.format(loginAuthUrl, callback, DefaultConst.DEFAULT_SOURCE_STRING);
    }

    public static void gotoLogin() {
        BrowserUtil.browse(loginUrl());
    }

    public static String getLoginType() {
        var setting = DevPilotLlmSettingsState.getInstance();
        return setting.getLoginType().toLowerCase(Locale.ROOT);
    }

    public static void changeLoginStatus(boolean isLoggedIn) {
        var projects = ProjectManager.getInstance().getOpenProjects();

        for (var project : projects) {
            var service = project.getService(DevPilotChatToolWindowService.class);
            service.changeLoginStatus(isLoggedIn);
            DevPilotStatusBarBaseWidget.update(project, isLoggedIn ? DevPilotStatusEnum.LoggedIn : DevPilotStatusEnum.NotLoggedIn);
        }
    }

    public static boolean isLogin() {
        var setting = DevPilotLlmSettingsState.getInstance();
        var loginType = LoginTypeEnum.getLoginTypeEnum(setting.getLoginType());

        switch (loginType) {
            case WX:
                return WxAuthUtils.isLogin();
            case ZA:
                return ZaSsoUtils.isLogin(ZaSsoEnum.ZA);
            case ZA_TI:
                return ZaSsoUtils.isLogin(ZaSsoEnum.ZA_TI);
            default:
                return false;
        }
    }

    public static void logout() {
        var setting = DevPilotLlmSettingsState.getInstance();
        var loginType = LoginTypeEnum.getLoginTypeEnum(setting.getLoginType());

        switch (loginType) {
            case WX:
                WxAuthUtils.logout();
                break;
            case ZA:
                ZaSsoUtils.logout(ZaSsoEnum.ZA);
                break;
            case ZA_TI:
                ZaSsoUtils.logout(ZaSsoEnum.ZA_TI);
                break;
        }

        changeLoginStatus(false);
    }
}
