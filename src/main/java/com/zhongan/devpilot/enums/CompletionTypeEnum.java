package com.zhongan.devpilot.enums;

public enum CompletionTypeEnum {
    COMMENT("comment"),
    INLINE("inline"),
    CHAT_COMPLETION("chat_completion");

    private final String type;

    CompletionTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
