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

    public static boolean endsWithADot(Document doc, int positionBeforeSuggestionPrefix) {
        int begin = positionBeforeSuggestionPrefix - ".".length();
        if (begin < 0 || positionBeforeSuggestionPrefix > doc.getTextLength()) {
            return false;
        } else {
            String tail = doc.getText(new TextRange(begin, positionBeforeSuggestionPrefix));
            return ".".equals(tail);
        }
    }

    @NotNull
    public static Integer toInt(@Nullable Long aLong) {
        if (aLong == null) {
            return 0;
        }

        return Math.toIntExact(aLong);
    }

    public static List<String> asLines(String block) {
        return Arrays.stream(block.split("\n")).collect(Collectors.toList());
    }

    public static Future<?> executeThread(Runnable runnable) {
        if (isUnitTestMode()) {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        }
        return AppExecutorUtil.getAppExecutorService().submit(runnable);
    }

    public static Future<?> executeThread(Runnable runnable, long delay, TimeUnit timeUnit) {
        if (isUnitTestMode()) {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        }
        return AppExecutorUtil.getAppScheduledExecutorService().schedule(runnable, delay, timeUnit);
    }

    public static boolean isUnitTestMode() {
        return ApplicationManager.getApplication() == null
                || ApplicationManager.getApplication().isUnitTestMode();
    }

}
