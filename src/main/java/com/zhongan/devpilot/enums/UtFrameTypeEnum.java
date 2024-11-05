package com.zhongan.devpilot.enums;

public enum UtFrameTypeEnum {

    JUNIT5_MOCKITO(1, "JUnit 5", "Mockito"),

    JUNIT5_POWERMOCK(2, "JUnit 5", "PowerMock"),

    JUNIT4_MOCKITO(3, "JUnit 4", "Mockito"),

    JUNIT4_POWERMOCK(4, "JUnit 4", "PowerMock");

    private final int code;

    private final String utFrameType;

    private final String mockFrameType;

    UtFrameTypeEnum(int code, String utFrameType, String mockFrameType) {
        this.code = code;
        this.utFrameType = utFrameType;
        this.mockFrameType = mockFrameType;
    }

    public int getCode() {
        return code;
    }

    public String getUtFrameType() {
        return utFrameType;
    }

    public String getMockFrameType() {
        return mockFrameType;
    }

}
