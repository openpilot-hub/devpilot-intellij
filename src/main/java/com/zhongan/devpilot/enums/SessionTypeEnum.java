package com.zhongan.devpilot.enums;

import java.util.Objects;

public enum SessionTypeEnum {

    INDEPENDENT(0, "independent session"),
    MULTI_TURN(1, "multi-turn session ");

    private final Integer code;

    private final String desc;

    SessionTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static SessionTypeEnum getEnumByCode(Integer code) {
        if (Objects.isNull(code)) {
            return MULTI_TURN;
        }
        for (SessionTypeEnum type : SessionTypeEnum.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return MULTI_TURN;
    }
}