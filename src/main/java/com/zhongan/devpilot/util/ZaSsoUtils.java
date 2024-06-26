package com.zhongan.devpilot.util;

import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import static com.zhongan.devpilot.constant.DefaultConst.AUTH_INFO_BUILD_TEMPLATE;

public class ZaSsoUtils {
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

    public static String getSsoUserName() {
        return zaSsoUsername(getSsoEnum());
    }

    public static String buildAuthInfo(ZaSsoEnum zaSsoEnum) {
        if (!isLogin(zaSsoEnum)) {
            return null;
        }

        var settings = AIGatewaySettingsState.getInstance();

        switch (zaSsoEnum) {
            case ZA_TI:
                return String.format(
                        AUTH_INFO_BUILD_TEMPLATE,
                        zaSsoEnum.getName().toLowerCase(Locale.ROOT),
                        settings.getTiSsoToken(),
                        settings.getTiSsoUsername(),
                        System.currentTimeMillis());
            case ZA:
            default:
                return String.format(
                        AUTH_INFO_BUILD_TEMPLATE,
                        zaSsoEnum.getName().toLowerCase(Locale.ROOT),
                        settings.getSsoToken(),
                        settings.getSsoUsername(),
                        System.currentTimeMillis());
        }
    }
}
