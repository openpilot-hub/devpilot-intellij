//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.general;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Utils {
    public Utils() {
    }

    public static boolean endsWithADot(Document doc, int positionBeforeSuggestionPrefix) {
        int begin = positionBeforeSuggestionPrefix - ".".length();
        if (begin >= 0 && positionBeforeSuggestionPrefix <= doc.getTextLength()) {
            String tail = doc.getText(new TextRange(begin, positionBeforeSuggestionPrefix));
            return tail.equals(".");
        } else {
            return false;
        }
    }

    public static @NotNull Integer toInt(@Nullable Long aLong) {
        Integer var10000;
        if (aLong == null) {
            var10000 = 0;


            return var10000;
        } else {
            var10000 = Math.toIntExact(aLong);

            return var10000;
        }
    }

    public static List<String> asLines(String block) {
        return (List)Arrays.stream(block.split("\n")).collect(Collectors.toList());
    }

    public static Future<?> executeThread(Runnable runnable) {
        if (isUnitTestMode()) {
            runnable.run();
            return CompletableFuture.completedFuture((Object)null);
        } else {
            return AppExecutorUtil.getAppExecutorService().submit(runnable);
        }
    }

    public static Future<?> executeThread(Runnable runnable, long delay, TimeUnit timeUnit) {
        if (isUnitTestMode()) {
            runnable.run();
            return CompletableFuture.completedFuture((Object)null);
        } else {
            return AppExecutorUtil.getAppScheduledExecutorService().schedule(runnable, delay, timeUnit);
        }
    }

    public static boolean isUnitTestMode() {
        return ApplicationManager.getApplication() == null || ApplicationManager.getApplication().isUnitTestMode();
    }
}
