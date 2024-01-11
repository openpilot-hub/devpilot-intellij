package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.general.StaticConfig;

public class DebounceUtils {

    public static long getDebounceInterval() {
        //TODO 读取应用配置
//        return getDebounceMsFromCapabilities() ?: AppSettingsState.instance.debounceTime
        Long debounceMsFromCapabilities = getDebounceMsFromCapabilities();
        return debounceMsFromCapabilities != null ? debounceMsFromCapabilities : StaticConfig.DEBOUNCE_VALUE_1200;
    }

    public static Long getDebounceMsFromCapabilities() {
        return StaticConfig.DEBOUNCE_VALUE_3000;
    }

}