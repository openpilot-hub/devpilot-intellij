package com.zhongan.devpilot;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;

import java.util.Locale;

public class DevPilotVersion {
    public static String getDevPilotVersion() {
        var pluginId = PluginId.getId("com.zhongan.devPilot");
        var plugin = PluginManagerCore.getPlugin(pluginId);

        if (plugin != null) {
            return plugin.getVersion();
        }

        return null;
    }

    public static String getDefaultLanguage() {
        var name = getVersionName().toLowerCase(Locale.ROOT);

        if (name.contains("idea")) {
            return "java";
        } else if (name.contains("pycharm")) {
            return "python";
        } else if (name.contains("webstorm")) {
            return "javascript";
        } else if (name.contains("phpstorm")) {
            return "php";
        } else if (name.contains("goland")) {
            return "go";
        }

        return "java";
    }

    public static String getIdeaVersion() {
        return ApplicationInfo.getInstance().getFullVersion();
    }

    public static String getVersionName() {
        return ApplicationInfo.getInstance().getVersionName();
    }
}
