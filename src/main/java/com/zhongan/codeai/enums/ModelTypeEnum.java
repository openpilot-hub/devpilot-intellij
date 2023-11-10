package com.zhongan.codeai.enums;

public enum ModelTypeEnum {
    TYQW("TYQW", "ali/qwen"),
    GPT3_5("GPT-3.5", "azure/gpt-3.5-turbo");

    // model show name
    private final String name;
    // model code
    private final String code;

    ModelTypeEnum(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ModelTypeEnum fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ModelTypeEnum type : ModelTypeEnum.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
