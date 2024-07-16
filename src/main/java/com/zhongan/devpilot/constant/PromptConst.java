package com.zhongan.devpilot.constant;

public class PromptConst {

    private PromptConst() {
    }

    public static final String RESPONSE_FORMAT = "You are a coding expert.\n" +
            "You must obey ALL of the following rules:\n\n" +
            "- quote variable name with single backtick such as `name`.\n" +
            "- quote code block with triple backticks such as ```...```";

    public static final String ANSWER_IN_CHINESE = "\n\n请用中文回答";

    public static final String GENERATE_COMMIT = "Write a clean and comprehensive commit message that accurately summarizes the changes made in the given `git diff` output, following the best practices and conventional commit convention. Remember that your entire response will be directly used as the git commit message. The response should be in the language {locale}.";

    public static final String DIFF_PREVIEW = "This is the `git diff`:\n" + "{diff}";

    public final static String MOCK_WEB_MVC = "please use MockMvc to mock web requests, ";

}
