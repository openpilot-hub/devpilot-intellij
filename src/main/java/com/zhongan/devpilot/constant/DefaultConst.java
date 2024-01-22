package com.zhongan.devpilot.constant;

import com.zhongan.devpilot.util.DevPilotMessageBundle;

public class DefaultConst {

    public final static String MAX_TOKEN_EXCEPTION_MSG = DevPilotMessageBundle.get("devpilot.chatWindow.context.overflow");

    public final static int TOKEN_MAX_LENGTH = 4096;

    public final static int ENGLISH_CONTENT_MAX_LENGTH = 12288;

    public final static int CHINESE_CONTENT_MAX_LENGTH = 1638;

    public final static String DEFAULT_CODE_LANGUAGE = "java";

    public final static String AI_GATEWAY_INSTRUCT_COMPLETION = "/ai/test/azure/gpt-35-turbo-instruct/completions";

    @Deprecated
    public final static String AI_GATEWAY_INSTRUCT_COMPLETION_ACCESS_KEY_TEMP = "30bb0c2d46194cd5b11f892ded3c6fbc";

//    public final static String AI_GATEWAY_INSTRUCT_COMPLETION = "/devpilot/v1/completions";

}