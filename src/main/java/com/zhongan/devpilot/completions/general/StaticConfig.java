package com.zhongan.devpilot.completions.general;

public class StaticConfig {
    public static final int MAX_COMPLETIONS = 5;

    // 100 KB
    public static final int DEFALUT_MAX_OFFSET = 100000;

    // 100 KB
    public static final int MAX_OFFSET = 100000;

    // 200 B
    public static final int MIN_OFFSET = 200;

    // 2000 B
    public static final int MAX_CHAT_COMPLETION_MESSAGE_LENGTH = 2000;

    //1000
    public static final int MAX_INSTRUCT_COMPLETION_TOKENS = 1000;

    // 100 B
    public static final String MIN_CHAT_COMPLETION_MESSAGE_LENGTH = "200";

    public static final int MIN_USER_PREFIX_LENGTH = 5;

    public static final int MIN_DELAY_TIME_IN_MILLIS = 6000;

    public static final int MAX_DELAY_TIME_IN_MILLIS = 12000;

    public static final Long DEBOUNCE_VALUE_300 = 300L;

    public static final Long DEBOUNCE_VALUE_600 = 600L;

    public static final Long DEBOUNCE_VALUE_900 = 900L;

    public static final Long DEBOUNCE_VALUE_1200 = 1200L;

    public static final Long DEBOUNCE_VALUE_3000 = 3000L;

}
