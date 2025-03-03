package com.zhongan.devpilot.embedding.enums;

public enum DevPilotFileType {
    JAVA("java"),
    POM("xml");

    private final String extension;

    DevPilotFileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}