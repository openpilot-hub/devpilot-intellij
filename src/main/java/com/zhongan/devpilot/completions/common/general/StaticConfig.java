package com.zhongan.devpilot.completions.common.general;

import com.intellij.openapi.util.IconLoader;
import com.zhongan.devpilot.DevPilotIcons;

import javax.swing.*;

public class StaticConfig {
  public static final int MAX_COMPLETIONS = 5;
  public static final int COMPLETION_TIME_THRESHOLD = 1000;
  public static final int NEWLINE_COMPLETION_TIME_THRESHOLD = 3000;
  public static final int ADVERTISEMENT_MAX_LENGTH = 100;
  public static final int MAX_OFFSET = 100000; // 100 KB
  public static final String BRAND_NAME = "devpilot";

  public static Icon getDevpilotIcon() {
    return IconLoader.getIcon("/icons/devpilot.svg", DevPilotIcons.class);
  }

}
