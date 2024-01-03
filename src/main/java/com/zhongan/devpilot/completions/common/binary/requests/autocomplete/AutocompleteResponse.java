package com.zhongan.devpilot.completions.common.binary.requests.autocomplete;

import com.zhongan.devpilot.completions.common.binary.BinaryResponse;

public class AutocompleteResponse implements BinaryResponse {
  public String old_prefix;
  public ResultEntry[] results;
  public String[] user_message;
  public boolean is_locked;
}
