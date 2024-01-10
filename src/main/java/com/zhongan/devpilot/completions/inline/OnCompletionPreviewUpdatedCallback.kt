package com.zhongan.devpilot.completions.inline

import com.zhongan.devpilot.completions.prediction.DevPilotCompletion

interface OnCompletionPreviewUpdatedCallback {
    fun onCompletionPreviewUpdated(completion: DevPilotCompletion)
}
