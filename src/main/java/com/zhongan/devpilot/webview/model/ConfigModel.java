package com.zhongan.devpilot.webview.model;

public class ConfigModel {
    private String theme;

    private String locale;

    private String username;

    public ConfigModel(String theme, String locale, String username) {
        this.theme = theme;
        this.locale = locale;
        this.username = username;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
