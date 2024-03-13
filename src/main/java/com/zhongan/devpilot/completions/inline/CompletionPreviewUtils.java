//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;

public class CompletionPreviewUtils {
    public CompletionPreviewUtils() {
    }

    public static boolean hadSuffix(DevPilotCompletion currentCompletion) {
        return currentCompletion.getOldSuffix().trim().length() > 0;
    }

    public static boolean isSingleLine(DevPilotCompletion currentCompletion) {
        return !currentCompletion.getSuffix().trim().contains("\n");
    }

    public static boolean shouldRemoveSuffix(DevPilotCompletion currentCompletion) {
        return hadSuffix(currentCompletion) && isSingleLine(currentCompletion);
    }
}
