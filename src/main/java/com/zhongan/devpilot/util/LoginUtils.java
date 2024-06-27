package com.zhongan.devpilot.util;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.ProjectManager;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.LoginTypeEnum;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.TrialServiceSettingsState;
import com.zhongan.devpilot.statusBar.DevPilotStatusBarBaseWidget;
import com.zhongan.devpilot.statusBar.status.DevPilotStatusEnum;

import java.util.Base64;
import java.util.Locale;

import org.jetbrains.ide.BuiltInServerManager;

import static com.zhongan.devpilot.constant.DefaultConst.AUTH_ON;
import static com.zhongan.devpilot.constant.DefaultConst.LOGIN_AUTH_URL;
import static com.zhongan.devpilot.constant.DefaultConst.LOGIN_CALLBACK_URL;

public class LoginUtils {
    public static String loginUrl() {
        var callback = String.format(LOGIN_CALLBACK_URL, BuiltInServerManager.getInstance().getPort());
        return String.format(LOGIN_AUTH_URL, callback, DefaultConst.DEFAULT_SOURCE_STRING);
    }

    public static void gotoLogin() {
        BrowserUtil.browse(loginUrl());
    }

    public static String getLoginType() {
        if (!isAuthOn()) {
            return "";
        }

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
        if (!isAuthOn()) {
            return true;
        }

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
        if (!isAuthOn()) {
            return;
        }

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

    public static String getUsername() {
        if (!isAuthOn()) {
            return "user";
        }

        if ("wx".equals(LoginUtils.getLoginType())) {
            String prefix = DevPilotMessageBundle.get("devpilot.status.account.wx");
            String wxUserId = TrialServiceSettingsState.getInstance().getWxUserId();
            return prefix + wxUserId.substring(wxUserId.length() - 4);

        } else {
            return ZaSsoUtils.getSsoUserName();
        }
    }

    public static String buildAuthInfo() {
        if (!isAuthOn()) {
            return null;
        }

        var setting = DevPilotLlmSettingsState.getInstance();
        var loginType = LoginTypeEnum.getLoginTypeEnum(setting.getLoginType());

        switch (loginType) {
            case WX:
                return WxAuthUtils.buildAuthInfo();
            case ZA:
                return ZaSsoUtils.buildAuthInfo(ZaSsoEnum.ZA);
            case ZA_TI:
                return ZaSsoUtils.buildAuthInfo(ZaSsoEnum.ZA_TI);
        }

        return null;
    }

    public static String buildAuthUrl(String baseUrl) {
        String encodedString = "";
        var paramString = buildAuthInfo();

        if (paramString != null) {
            encodedString = "?token=" + Base64.getEncoder().encodeToString(paramString.getBytes());
        }

        return baseUrl + encodedString;
    }

    // is auth turn on
    public static boolean isAuthOn() {
        return AUTH_ON;
    }
}
