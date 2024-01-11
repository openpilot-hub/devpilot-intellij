package com.zhongan.devpilot.completions.general;

import com.intellij.openapi.util.IconLoader;
import com.zhongan.devpilot.DevPilotIcons;

import javax.swing.*;

public class StaticConfig {
    public static final int MAX_COMPLETIONS = 5;

    // 100 KB
    public static final int DEFALUT_MAX_OFFSET = 100000;

    // 100 KB
    public static final int MAX_OFFSET = 100000;

    // 100 B
    public static final int MIN_OFFSET = 100;

    // 100 KB
    public static final int MAX_CHAT_COMPLETION_MESSAGE_LENGTH = 100000;

    // 100 B
    public static final int MIN_CHAT_COMPLETION_MESSAGE_LENGTH = 100;

    public static final int MIN_USER_PREFIX_LENGTH = 5;

    public static final int MIN_DELAY_TIME_IN_MILLIS = 6000;
    public static final int MAX_DELAY_TIME_IN_MILLIS = 12000;


}
