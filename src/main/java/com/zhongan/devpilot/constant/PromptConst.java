package com.zhongan.devpilot.constant;

public class PromptConst {

    private PromptConst() {
    }

    public static final String RESPONSE_FORMAT = "You are a coding expert.\n" +
            "You must obey ALL of the following rules:\n\n" +
            "- quote variable name with single backtick such as `name`.\n" +
            "- quote code block with triple backticks such as ```...```";

    public static final String ANSWER_IN_CHINESE = "\nPlease answer in Chinese.";

    public static final String GENERATE_COMMIT = "Summarize the git diff with a concise and descriptive commit message. Adopt the imperative mood, present tense, active voice, and include relevant verbs. Remember that your entire response will be directly used as the git commit message.";
    
    public final static String MOCK_WEB_MVC = "please use MockMvc to mock web requests, ";

}
