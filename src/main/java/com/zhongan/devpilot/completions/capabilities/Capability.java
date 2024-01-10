package com.zhongan.devpilot.completions.capabilities;

import com.google.gson.annotations.SerializedName;

public enum Capability {
    @SerializedName("inline_suggestions_mode")
    INLINE_SUGGESTIONS,
    @SerializedName("alpha")
    ALPHA,
    @SerializedName("use_hybrid_inline_popup")
    USE_HYBRID_INLINE_POPUP,
    @SerializedName("debounce_value_300")
    DEBOUNCE_VALUE_300,
    @SerializedName("debounce_value_600")
    DEBOUNCE_VALUE_600,
    @SerializedName("debounce_value_900")
    DEBOUNCE_VALUE_900,
    @SerializedName("debounce_value_1200")
    DEBOUNCE_VALUE_1200,
    @SerializedName("debounce_value_1500")
    DEBOUNCE_VALUE_1500,
    @SerializedName("plugin.feature.force_registration")
    FORCE_REGISTRATION,
    @SerializedName("plugin.feature.devpilot_chat")
    DEVPILOT_CHAT,
    @SerializedName("preview")
    PREVIEW_CAPABILITY,
    @SerializedName("preview_ended")
    PREVIEW_ENDED_CAPABILITY,
}
