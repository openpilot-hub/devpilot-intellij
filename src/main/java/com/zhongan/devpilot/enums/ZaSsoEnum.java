package com.zhongan.devpilot.enums;

public enum ZaSsoEnum {
    ZA("ZA", "众安保险SSO"),
    ZA_TI("ZA_TI", "众安国际SSO");

    // sso name
    private final String name;

    // sso display name
    private final String displayName;

    ZaSsoEnum(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ZaSsoEnum fromName(String name) {
        if (name == null) {
            return ZA;
        }
        for (ZaSsoEnum type : ZaSsoEnum.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return ZA;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
