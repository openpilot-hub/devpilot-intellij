package com.zhongan.devpilot.util;

import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.settings.state.TrialServiceSettingsState;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.ide.BuiltInServerManager;

public class WxAuthUtils {
    private static final String wxAuthUrl = "https://devpilot-h5.zhongan.com/login?scope=%s&backUrl=%s&source=%s";

    private static final String wxCallbackUrl = "http://127.0.0.1:%s/wx/callback";

    @Deprecated
    public static String getWxAuthUrl() {
        var backUrl = String.format(wxCallbackUrl, BuiltInServerManager.getInstance().getPort());
        return String.format(wxAuthUrl, "gzh", backUrl, DefaultConst.DEFAULT_SOURCE_STRING);
    }

    public static boolean isLogin() {
        var settings = TrialServiceSettingsState.getInstance();
        return StringUtils.isNotBlank(settings.getWxToken())
                && StringUtils.isNotBlank(settings.getWxUsername())
                && StringUtils.isNotBlank(settings.getWxUserId());
    }

    public static void logout() {
        var setting = TrialServiceSettingsState.getInstance();
        setting.setWxToken(null);
        setting.setWxUsername(null);
        setting.setWxUserId(null);
    }

    public static void login(String token, String username, String userId) {
        var setting = TrialServiceSettingsState.getInstance();
        setting.setWxToken(token);
        setting.setWxUsername(username);
        setting.setWxUserId(userId);
    }
}
