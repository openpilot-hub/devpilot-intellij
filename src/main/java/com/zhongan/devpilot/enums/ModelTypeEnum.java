package com.zhongan.devpilot.enums;

public enum ModelTypeEnum {
    GPT3_5("GPT-3.5", "azure/gpt-3.5-turbo", "GPT-3.5"),
    ERNIE("Ernie", "baidu/ernie-bot-4", "Ernie Bot"),
    TYQW("TYQW", "ali/qwen-plus", "Qwen"),
    SENSENOVA("sensenova", "sensenova/nova-ptc-xl-v1", "Sense Nova");

    // model name
    private final String name;

    // model code
    private final String code;

    // model display name
    private final String displayName;

    ModelTypeEnum(String name, String code, String displayName) {
        this.name = name;
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ModelTypeEnum fromName(String name) {
        if (name == null) {
            return GPT3_5;
        }
        for (ModelTypeEnum type : ModelTypeEnum.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return GPT3_5;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
