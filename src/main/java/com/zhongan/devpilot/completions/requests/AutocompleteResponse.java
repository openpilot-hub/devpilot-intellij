package com.zhongan.devpilot.completions.requests;

public class AutocompleteResponse {
    public String old_prefix;
    public ResultEntry[] results;
    public String[] user_message;
    public boolean is_locked;
}
