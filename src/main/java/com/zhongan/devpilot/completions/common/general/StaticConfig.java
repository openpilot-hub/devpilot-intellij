package com.zhongan.devpilot.completions.common.general;

import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.IconLoader;
import com.zhongan.devpilot.DevPilotIcons;

import javax.swing.*;

public class StaticConfig {
  public static final String DEVPILOT_PLUGIN_ID_RAW = "com.devpilot.devpilot";
  public static final PluginId DEVPILOT_PLUGIN_ID = PluginId.getId(DEVPILOT_PLUGIN_ID_RAW);
  public static final int MAX_COMPLETIONS = 5;
  public static final int COMPLETION_TIME_THRESHOLD = 1000;
  public static final int NEWLINE_COMPLETION_TIME_THRESHOLD = 3000;
  public static final int ADVERTISEMENT_MAX_LENGTH = 100;
  public static final int MAX_OFFSET = 100000; // 100 KB
  public static final int BINARY_TIMEOUTS_THRESHOLD_MILLIS = 60_000;
  public static final String BRAND_NAME = "devpilot";
  public static final String LIMITATION_SYMBOL = "🔒";

  public static Icon getDevpilotIcon() {
    return IconLoader.getIcon("/icons/devpilot.svg", DevPilotIcons.class);
  }

}
