package com.zhongan.devpilot.enums;

public enum LoginTypeEnum {
    ZA("ZA", "众安保险SSO"),
    ZA_TI("ZA_TI", "众安国际SSO"),
    WX("WX", "微信公众号");

    // login type
    private final String type;

    private final String displayName;

    LoginTypeEnum(String type, String displayName) {
        this.type = type;
        this.displayName = displayName;
    }

    public static LoginTypeEnum getLoginTypeEnum(String type) {
        for (LoginTypeEnum loginTypeEnum : LoginTypeEnum.values()) {
            if (loginTypeEnum.getType().equals(type)) {
                return loginTypeEnum;
            }
        }
        return LoginTypeEnum.ZA;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }
}
