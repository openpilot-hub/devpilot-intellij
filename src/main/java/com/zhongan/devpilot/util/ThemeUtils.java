package com.zhongan.devpilot.util;

import javax.swing.UIManager;

public class ThemeUtils {
    public static boolean isDarkTheme() {
        if (JetbrainsVersionUtils.isVersionLaterThan233()) {
            var lookAndFeelDefaults = UIManager.getLookAndFeelDefaults();
            return lookAndFeelDefaults == null || lookAndFeelDefaults.getBoolean("ui.theme.is.dark");
        }

        return UIManager.getLookAndFeel().getName().contains("Darcula");
    }

    public static String themeType() {
        return isDarkTheme() ? "dark" : "light";
    }
}
