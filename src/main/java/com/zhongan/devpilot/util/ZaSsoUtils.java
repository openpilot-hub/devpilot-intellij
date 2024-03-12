package com.zhongan.devpilot.util;

import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.ide.BuiltInServerManager;

public class ZaSsoUtils {
    private static final String ssoAuthUrl = "https://devpilot-h5.zhongan.com/login?scope=%s&backUrl=%s&source=%s";

    private static final String ssoCallbackUrl = "http://127.0.0.1:%s/za/sso/callback";

    @Deprecated
    public static String getZaSsoAuthUrl(ZaSsoEnum ssoEnum) {
        var port = BuiltInServerManager.getInstance().getPort();
        var scope = ssoEnum == ZaSsoEnum.ZA_TI ? "zati" : "za";

        var backUrl = String.format(ssoCallbackUrl, port);

        return String.format(ssoAuthUrl, scope, backUrl, DefaultConst.DEFAULT_SOURCE_STRING);
    }

    public static boolean isLogin(ZaSsoEnum zaSsoEnum) {
        var settings = AIGatewaySettingsState.getInstance();
        switch (zaSsoEnum) {
            case ZA_TI:
                return StringUtils.isNotBlank(settings.getTiSsoToken()) && StringUtils.isNotBlank(settings.getTiSsoUsername());
            case ZA:
            default:
                return StringUtils.isNotBlank(settings.getSsoToken()) && StringUtils.isNotBlank(settings.getSsoUsername());
        }
    }

    public static void login(ZaSsoEnum zaSsoEnum, String token, String username) {
        var settings = AIGatewaySettingsState.getInstance();
        settings.setSelectedSso(zaSsoEnum.getName());
        switch (zaSsoEnum) {
            case ZA_TI:
                settings.setTiSsoToken(token);
                settings.setTiSsoUsername(username);
                break;
            case ZA:
            default:
                settings.setSsoToken(token);
                settings.setSsoUsername(username);
                break;
        }
    }

    public static String zaSsoUsername(ZaSsoEnum zaSsoEnum) {
        var settings = AIGatewaySettingsState.getInstance();
        switch (zaSsoEnum) {
            case ZA_TI:
                return settings.getTiSsoUsername();
            case ZA:
            default:
                return settings.getSsoUsername();
        }
    }

    public static void logout(ZaSsoEnum zaSsoEnum) {
        var settings = AIGatewaySettingsState.getInstance();
        switch (zaSsoEnum) {
            case ZA_TI:
                settings.setTiSsoUsername(null);
                settings.setTiSsoToken(null);
                break;
            case ZA:
            default:
                settings.setSsoUsername(null);
                settings.setSsoToken(null);
                break;
        }
    }

    public static String getSsoType() {
        var settings = AIGatewaySettingsState.getInstance();
        return settings.getSelectedSso().toLowerCase(Locale.ROOT);
    }

    public static ZaSsoEnum getSsoEnum() {
        var settings = AIGatewaySettingsState.getInstance();
        return ZaSsoEnum.fromName(settings.getSelectedSso());
    }
}
