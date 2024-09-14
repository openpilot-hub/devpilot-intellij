package com.zhongan.devpilot.enums;

import java.util.Objects;

public enum EditorActionEnum {

    GENERATE_COMMENTS("devpilot.action.generate.comments", "Generate comments in the following code"),

    COMMENT_METHOD("devpilot.action.generate.method.comments", ""),

    GENERATE_TESTS("devpilot.action.generate.tests", "Generate Tests in the following code"),

    FIX_CODE("devpilot.action.fix", "Fix This in the following code"),

    EXPLAIN_CODE("devpilot.action.explain", "Explain this in the following code"),

    COMPLETE_CODE("devpilot.action.completions", "code completions");

    private final String label;

    private final String userMessage;

    EditorActionEnum(String label, String userMessage) {
        this.label = label;
        this.userMessage = userMessage;
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

    public static EditorActionEnum getEnumByName(String name) {
        if (Objects.isNull(name)) {
            return null;
        }
        for (EditorActionEnum type : EditorActionEnum.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
