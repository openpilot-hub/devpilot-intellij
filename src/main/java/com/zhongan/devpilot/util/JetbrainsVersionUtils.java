package com.zhongan.devpilot.util;

import com.intellij.openapi.application.ApplicationInfo;

public class JetbrainsVersionUtils {
    public static String getJetbrainsBuildVersion() {
        return ApplicationInfo.getInstance().getBuild().asString();
    }

    public static int getJetbrainsBaselineVersion() {
        return ApplicationInfo.getInstance().getBuild().getBaselineVersion();
    }

    public static int[] getJetbrainsBuildVersionArr() {
        return ApplicationInfo.getInstance().getBuild().getComponents();
    }

    // version >= 2023.3 (build version 233.11799.241)
    public static boolean isVersionLaterThan233() {
        var versionList = getJetbrainsBuildVersionArr();

        var baseVersion = versionList[0];

        if (baseVersion == 232) {
            return versionList[1] >= 11799;
        }

        return baseVersion > 232;
    }
}
