package com.zhongan.devpilot.completions.requests;

public class AutocompleteResponse {
    public String oldPrefix;

    public ResultEntry[] results;

    public String[] userMessage;

    public boolean isLocked;
}
