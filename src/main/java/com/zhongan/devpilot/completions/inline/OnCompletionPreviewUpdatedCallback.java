package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;

public interface OnCompletionPreviewUpdatedCallback {
    void onCompletionPreviewUpdated(DevPilotCompletion completion);
}
