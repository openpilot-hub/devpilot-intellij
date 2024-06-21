package com.zhongan.devpilot.util;

import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

public class ConfigBundleUtils {
    private static final ResourceBundle bundle;

    static {
        ResourceBundle tmp;
        try {
            tmp = ResourceBundle.getBundle("config.local");
        } catch (Exception e) {
            tmp = null;
        }
        bundle = tmp;
    }

    public static String getConfig(String key) {
        return getConfig(key, null);
    }

    public static String getConfig(String key, String defaultValue) {
        if (bundle == null) {
            return defaultValue;
        }

        String value = bundle.getString(key);

        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        return value;
    }
}
