package com.zhongan.codeai;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;

public class OpenPilotVersion {
    public static String getOpenPilotVersion() {
        var pluginId = PluginId.getId("com.zhongan.openPilot");
        var plugin = PluginManagerCore.getPlugin(pluginId);

        if (plugin != null) {
            return plugin.getVersion();
        }

        return null;
    }

    public static String getIdeaVersion() {
        return ApplicationInfo.getInstance().getFullVersion();
    }
}
