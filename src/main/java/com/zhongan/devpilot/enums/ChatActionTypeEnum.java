package com.zhongan.devpilot.enums;

/**
 * Used for telemetry upload
 */
public enum ChatActionTypeEnum {
    INSERT("INSERT"),
    REPLACE("REPLACE"),
    NEW_FILE("NEW_FILE"),
    COPY("COPY");

    private final String type;

    ChatActionTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
