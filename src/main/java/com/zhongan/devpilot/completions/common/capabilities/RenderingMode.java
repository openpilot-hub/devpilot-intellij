package com.zhongan.devpilot.completions.common.capabilities;

import com.google.gson.annotations.SerializedName;

/**
 * Used to mark which rendering option was actually used by the UI for - to be sent in the selection
 * report
 */
public enum RenderingMode {
  @SerializedName(value = "Inline")
  INLINE,
  @SerializedName(value = "Popup")
  AUTOCOMPLETE,
}
