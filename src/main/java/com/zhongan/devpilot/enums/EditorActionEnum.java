package com.zhongan.devpilot.enums;

import java.util.Objects;

public enum EditorActionEnum {
    PERFORMANCE_CHECK("devpilot.action.performance.check", "Performance check in the following code",
        "{{selectedCode}}\nGiving the code above, please fix any performance issues." +
            "\nRemember you are very familiar with performance optimization.\n"),

    GENERATE_COMMENTS("devpilot.action.generate.comments", "Generate comments in the following code",
        "{{selectedCode}}\nGiving the code above, please generate code comments, return code with comments."),

        GENERATE_TESTS("devpilot.action.generate.tests", "Generate Tests in the following code",
            "{{selectedCode}}\nGiving the {{language:unknown}} code above, " +
                "please help to generate {{testFramework:suitable}} test cases for it, " +
            "mocking test data with {{mockFramework:suitable mock framework}} if necessary, " +
            "{{additionalMockPrompt:}}" +
            "be aware that if the code is untestable, " +
            "please state it and give suggestions instead."),

    FIX_THIS("devpilot.action.fix", "Fix This in the following code",
        "{{selectedCode}}\nGiving the code above, please help to fix it:\n\n" +
            "- Fix any typos or grammar issues.\n" +
            "- Use better names as replacement to magic numbers or arbitrary acronyms.\n" +
            "- Simplify the code so that it's more strait forward and easy to understand.\n" +
            "- Optimize it for performance reasons.\n" +
            "- Refactor it using best practice in software engineering.\n" + "\nMust only provide the code to be fixed and explain why it should be fixed.\n"),

    REVIEW_CODE("devpilot.action.review", "Review code in the following code",
        "{{selectedCode}}\nGiving the code above, please review the code line by line:\n\n" +
            "- Think carefully, you should be extremely careful.\n" +
            "- Find out if any bugs exists.\n" +
            "- Reveal any bad smell in the code.\n" +
            "- Give optimization or best practice suggestion.\n"),

    EXPLAIN_THIS("devpilot.action.explain", "Explain this in the following code",
        "{{selectedCode}}\nGiving the code above, please explain it in detail, line by line.\n"),
    CODE_COMPLETIONS("devpilot.action.completions", "code completions", "You are a code completion service, please try to auto complete the code below at {{offsetCode}}, only output the code, don't try to have conversation with user.```{{selectedCode}}```");;


    private final String label;

    private final String userMessage;

    private final String prompt;

    EditorActionEnum(String label, String userMessage, String prompt) {
        this.label = label;
        this.userMessage = userMessage;
        this.prompt = prompt;
    }

    public static EditorActionEnum getEnumByLabel(String label) {
        if (Objects.isNull(label)) {
            return null;
        }
        for (EditorActionEnum type : EditorActionEnum.values()) {
            if (type.getLabel().equals(label)) {
                return type;
            }
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
