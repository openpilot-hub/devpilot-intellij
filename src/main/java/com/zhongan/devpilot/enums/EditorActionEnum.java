package com.zhongan.devpilot.enums;

import java.util.Objects;

public enum EditorActionEnum {

    GENERATE_COMMENTS("devpilot.action.generate.comments", "devpilot.inlay.shortcut.inlineComment", "Generate comments in the following code"),

    COMMENT_METHOD("devpilot.action.generate.method.comments", "devpilot.inlay.shortcut.methodComments", ""),

    GENERATE_TESTS("devpilot.action.generate.tests", "devpilot.inlay.shortcut.test", "Generate Tests in the following code"),

    FIX_CODE("devpilot.action.fix", "devpilot.inlay.shortcut.fix", "Fix This in the following code"),

    EXPLAIN_CODE("devpilot.action.explain", "devpilot.inlay.shortcut.explain", "Explain this in the following code"),

    COMPLETE_CODE("devpilot.action.completions", "", "code completions");

    private final String label;

    private final String inlayLabel;

    private final String userMessage;

    EditorActionEnum(String label, String inlayLabel, String userMessage) {
        this.label = label;
        this.inlayLabel = inlayLabel;
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

    public String getLabel() {
        return label;
    }

    public String getInlayLabel() {
        return inlayLabel;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
