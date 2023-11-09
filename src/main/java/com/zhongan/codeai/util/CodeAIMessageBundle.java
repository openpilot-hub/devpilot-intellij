package com.zhongan.codeai.util;

import com.intellij.DynamicBundle;
import com.zhongan.codeai.settings.state.LanguageSettingsState;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class CodeAIMessageBundle extends DynamicBundle {

    private static final CodeAIMessageBundle INSTANCE = new CodeAIMessageBundle();

    private static ResourceBundle bundleEn;

    private static ResourceBundle bundleCN;

    private static final String pathToBundle = "messages.codeai";

    private CodeAIMessageBundle() {
        super(pathToBundle);
    }

    static {
        bundleEn = ResourceBundle.getBundle(pathToBundle, Locale.ENGLISH);
        bundleCN = ResourceBundle.getBundle(pathToBundle, Locale.CHINA);
    }

    public static String get(@NotNull @PropertyKey(resourceBundle = "messages.codeai") String key) {
        Integer languageIndex = LanguageSettingsState.getInstance().getLanguageIndex();
        if (languageIndex == 1) {
            return bundleCN.getString(key);
        }
        return bundleEn.getString(key);
    }

    public static String get(@NotNull @PropertyKey(resourceBundle = "messages.codeai") String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static String getFromBundleEn(@NotNull @PropertyKey(resourceBundle = "messages.codeai") String key) {
        return bundleEn.getString(key);
    }

}