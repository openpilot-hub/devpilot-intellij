package com.zhongan.devpilot.util;

import com.zhongan.devpilot.settings.state.TrialServiceSettingsState;

import org.apache.commons.lang3.StringUtils;

public class WxAuthUtils {
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
