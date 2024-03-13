//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.general.StaticConfig;

public class DebounceUtils {
    public DebounceUtils() {
    }

    public static long getDebounceInterval() {
        Long debounceMsFromCapabilities = getDebounceMsFromCapabilities();
        return debounceMsFromCapabilities != null ? debounceMsFromCapabilities : StaticConfig.DEBOUNCE_VALUE_1200;
    }

    public static Long getDebounceMsFromCapabilities() {
        return StaticConfig.DEBOUNCE_VALUE_300;
    }
}
