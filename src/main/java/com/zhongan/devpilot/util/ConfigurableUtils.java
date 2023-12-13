package com.zhongan.devpilot.util;

import com.zhongan.devpilot.settings.DevPilotSettingsComponent;

public class ConfigurableUtils {
    private static DevPilotSettingsComponent component;

    public static void setConfigurableCache(DevPilotSettingsComponent component) {
        ConfigurableUtils.component = component;
    }

    public static DevPilotSettingsComponent getConfigurableCache() {
        return ConfigurableUtils.component;
    }
}
