package com.zhongan.devpilot.constant;

import com.zhongan.devpilot.util.DevPilotMessageBundle;

public class DefaultConst {

    private DefaultConst() {
    }

    public static final String GPT_35_MAX_TOKEN_EXCEPTION_MSG = DevPilotMessageBundle.get("devpilot.chatWindow.context.overflow");

    public static final int GPT_35_TOKEN_MAX_LENGTH = 16384;

    public static final int CONVERSATION_WINDOW_LENGTH = 8;

    public static final int ENGLISH_CONTENT_MAX_LENGTH = 12288;

    public static final int CHINESE_CONTENT_MAX_LENGTH = 1638;

    public static final String DEFAULT_CODE_LANGUAGE = "java";

    public static final String DEFAULT_SOURCE_STRING = "JetBrains IDE";

    public static final String AI_GATEWAY_INSTRUCT_COMPLETION = "/devpilot/v1/code-completion/default";

}