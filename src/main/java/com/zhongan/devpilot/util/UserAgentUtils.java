package com.zhongan.devpilot.util;

import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.enums.LoginTypeEnum;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.TrialServiceSettingsState;

public class UserAgentUtils {
    public static String buildUserAgent() {
        var settings = DevPilotLlmSettingsState.getInstance();
        var loginType = LoginTypeEnum.getLoginTypeEnum(settings.getLoginType());

        switch (loginType) {
            case ZA:
                return getUserAgent(ZaSsoEnum.ZA);
            case ZA_TI:
                return getUserAgent(ZaSsoEnum.ZA_TI);
            case WX:
                return getWxUserAgent();
        }

        return getWxUserAgent();
    }

    public static String getUserAgent(ZaSsoEnum zaSsoEnum) {
        String token, username;

        var settings = AIGatewaySettingsState.getInstance();

        switch (zaSsoEnum) {
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
        return String.format("%s-%s|%s|%s|%s", DevPilotVersion.getVersionName(), DevPilotVersion.getIdeaVersion(),
                DevPilotVersion.getDevPilotVersion(), token, username);
    }

    public static String getWxUserAgent() {
        var settings = TrialServiceSettingsState.getInstance();

        // format: idea version|plugin version|token|userid
        return String.format("%s-%s|%s|%s|%s", DevPilotVersion.getVersionName(), DevPilotVersion.getIdeaVersion(),
                DevPilotVersion.getDevPilotVersion(), settings.getWxToken(), settings.getWxUserId());
    }
}
