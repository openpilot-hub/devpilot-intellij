package com.zhongan.devpilot.enums;

public enum OpenAIModelNameEnum {
    GPT3_5_TURBO("gpt-3.5-turbo", "gpt-3.5-turbo"),
    GPT3_5_TURBO_16K("gpt-3.5-turbo-16k", "gpt-3.5-turbo(16k)"),
    GPT4("gpt-4", "gpt-4"),
    GPT4_32K("gpt-4-32k", "gpt-4(32k)"),
    CUSTOM("custom", "Custom Model");

    private String name;

    private String displayName;

    OpenAIModelNameEnum(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public static OpenAIModelNameEnum fromName(String name) {
        if (name == null) {
            return GPT3_5_TURBO;
        }
        for (OpenAIModelNameEnum type : OpenAIModelNameEnum.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return GPT3_5_TURBO;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
