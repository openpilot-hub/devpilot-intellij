package com.zhongan.devpilot.common.general;

import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.IconLoader;
import javax.swing.*;

public class StaticConfig {
  // Must be identical to what is written under <id>com.devpilot.DEVPILOT</id> in plugin.xml !!!
  public static final String DEVPILOT_PLUGIN_ID_RAW = "com.devpilot.devpilot";
  public static final PluginId DEVPILOT_PLUGIN_ID = PluginId.getId(DEVPILOT_PLUGIN_ID_RAW);
  public static final int MAX_COMPLETIONS = 5;
  public static final int COMPLETION_TIME_THRESHOLD = 1000;
  public static final int NEWLINE_COMPLETION_TIME_THRESHOLD = 3000;
  public static final int ADVERTISEMENT_MAX_LENGTH = 100;
  public static final int MAX_OFFSET = 100000; // 100 KB
  public static final String SET_STATE_RESPONSE_RESULT_STRING = "Done";
  public static final int BINARY_TIMEOUTS_THRESHOLD_MILLIS = 60_000;
  public static final String BRAND_NAME = "devpilot";
  public static final String ICON_AND_NAME_PATH = "icons/devpilot-starter-13px.png";
  public static final String LIMITATION_SYMBOL = "🔒";

  public static Icon getDevpilotIcon() {
    return IconLoader.findIcon("devpilot.png");
  }

}
