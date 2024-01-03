package com.zhongan.devpilot.completions.common.general;

import static com.zhongan.devpilot.completions.common.general.StaticConfig.DEVPILOT_PLUGIN_ID;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Utils {
  private static final String UNKNOWN = "Unknown";

  public static String getDevPilotPluginVersion() {
    return getDevPilotPluginDescriptor().map(IdeaPluginDescriptor::getVersion).orElse(UNKNOWN);
  }

  @NotNull
  public static Optional<IdeaPluginDescriptor> getDevPilotPluginDescriptor() {
    return Arrays.stream(PluginManager.getPlugins())
        .filter(plugin -> StaticConfig.DEVPILOT_PLUGIN_ID.equals(plugin.getPluginId()))
        .findAny();
  }

  public static boolean endsWithADot(Document doc, int positionBeforeSuggestionPrefix) {
    int begin = positionBeforeSuggestionPrefix - ".".length();
    if (begin < 0 || positionBeforeSuggestionPrefix > doc.getTextLength()) {
      return false;
    } else {
      String tail = doc.getText(new TextRange(begin, positionBeforeSuggestionPrefix));
      return tail.equals(".");
    }
  }

  @NotNull
  public static String readContent(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;

    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }

    return result.toString(StandardCharsets.UTF_8.name()).trim();
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

  public static String cmdSanitize(String text) {
    return text.replace(" ", "");
  }

  public static String wrapWithHtml(String content) {
    return wrapWithHtmlTag(content, "html");
  }

  public static String wrapWithHtmlTag(String content, String tag) {
    return "<" + tag + ">" + content + "</" + tag + ">";
  }

  public static long getDaysDiff(Date date1, Date date2) {
    if (date1 != null && date2 != null) {
      return TimeUnit.DAYS.convert(
          Math.abs(date2.getTime() - date1.getTime()), TimeUnit.MILLISECONDS);
    }
    return -1;
  }

  public static long getHoursDiff(Date date1, Date date2) {
    if (date1 != null && date2 != null) {
      return TimeUnit.HOURS.convert(date2.getTime() - date1.getTime(), TimeUnit.MILLISECONDS);
    }
    return -1;
  }

  public static Future<?> executeUIThreadWithDelay(
      Runnable runnable, long delay, TimeUnit timeUnit) {
    return executeThread(
        () -> ApplicationManager.getApplication().invokeLater(runnable), delay, timeUnit);
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

  public static String trimEndSlashAndWhitespace(String text) {
    return text.replace("/*\\s*$", "");
  }

  public static void setCustomRepository(String url) {
    if (!url.trim().isEmpty()) {
      List<String> pluginHosts = UpdateSettings.getInstance().getStoredPluginHosts();
      String newStore =
          String.format("%s/update/jetbrains/updatePlugins.xml", trimEndSlashAndWhitespace(url));
      pluginHosts.add(newStore);
      Logger.getInstance(Utils.class)
          .debug(String.format("Added custom repository to %s", newStore));
      ContainerUtil.removeDuplicates(pluginHosts);
    }
  }

  public static void replaceCustomRepository(String oldUrl, String newUrl) {
    List<String> pluginHosts = UpdateSettings.getInstance().getStoredPluginHosts();
    if (!newUrl.trim().isEmpty()) {
      String newStore =
          String.format("%s/update/jetbrains/updatePlugins.xml", trimEndSlashAndWhitespace(newUrl));
      pluginHosts.add(newStore);
      Logger.getInstance(Utils.class)
          .debug(String.format("Added custom repository to %s", newStore));
    }
    if (!oldUrl.trim().isEmpty()) {
      String oldPluginRepo =
          String.format("%s/update/jetbrains/updatePlugins.xml", trimEndSlashAndWhitespace(oldUrl));
      pluginHosts.remove(oldPluginRepo);
      Logger.getInstance(Utils.class)
          .debug(String.format("Removed custom repository from %s", oldPluginRepo));
    }
    ContainerUtil.removeDuplicates(pluginHosts);
  }
}
