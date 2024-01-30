package com.zhongan.devpilot.util;

import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.TrialServiceSettingsState;

public class UserAgentUtils {
    public static String getUserAgent() {
        String token, username;

        var settings = AIGatewaySettingsState.getInstance();
        var selectedSso = settings.getSelectedSso();
        var selectedSsoNum = ZaSsoEnum.fromName(selectedSso);

        switch (selectedSsoNum) {
            case ZA_TI:
                token = settings.getTiSsoToken();
                username = settings.getTiSsoUsername();
                break;
            case ZA:
            default:
                token = settings.getSsoToken();
                username = settings.getSsoUsername();
                break;
        }

        // format: idea version|plugin version|user token|username
        return String.format("idea-%s|%s|%s|%s", DevPilotVersion.getIdeaVersion(),
                DevPilotVersion.getDevPilotVersion(), token, username);
    }

    public static String getWxUserAgent() {
        var settings = TrialServiceSettingsState.getInstance();

        // format: idea version|plugin version|token|userid
        return String.format("idea-%s|%s|%s|%s", DevPilotVersion.getIdeaVersion(),
                DevPilotVersion.getDevPilotVersion(), settings.getWxToken(), settings.getWxUserId());
    }
}
