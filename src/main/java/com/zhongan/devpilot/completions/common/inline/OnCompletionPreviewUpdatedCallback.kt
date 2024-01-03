package com.zhongan.devpilot.completions.common.inline

import com.zhongan.devpilot.completions.common.prediction.DevPilotCompletion

interface OnCompletionPreviewUpdatedCallback {
    fun onCompletionPreviewUpdated(completion: DevPilotCompletion)
}
