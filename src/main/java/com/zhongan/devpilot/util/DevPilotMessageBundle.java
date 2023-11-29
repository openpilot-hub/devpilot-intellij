package com.zhongan.devpilot.util;

import com.intellij.DynamicBundle;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class DevPilotMessageBundle extends DynamicBundle {

    private static final DevPilotMessageBundle INSTANCE = new DevPilotMessageBundle();

    private static ResourceBundle bundleEn;

    private static ResourceBundle bundleCN;

    private static final String pathToBundle = "messages.devpilot";

    private DevPilotMessageBundle() {
        super(pathToBundle);
    }

    static {
        bundleEn = ResourceBundle.getBundle(pathToBundle, Locale.ENGLISH);
        bundleCN = ResourceBundle.getBundle(pathToBundle, Locale.CHINA);
    }

    public static String get(@NotNull @PropertyKey(resourceBundle = "messages.devpilot") String key) {
        Integer languageIndex = LanguageSettingsState.getInstance().getLanguageIndex();
        if (languageIndex == 1) {
            return bundleCN.getString(key);
        }
        return bundleEn.getString(key);
    }

    public static String get(@NotNull @PropertyKey(resourceBundle = "messages.devpilot") String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static String getFromBundleEn(@NotNull @PropertyKey(resourceBundle = "messages.devpilot") String key) {
        return bundleEn.getString(key);
    }

}