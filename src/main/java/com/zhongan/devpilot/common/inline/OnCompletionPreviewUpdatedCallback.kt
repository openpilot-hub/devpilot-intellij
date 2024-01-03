package com.zhongan.devpilot.common.inline

import com.zhongan.devpilot.common.prediction.DevPilotCompletion

interface OnCompletionPreviewUpdatedCallback {
    fun onCompletionPreviewUpdated(completion: DevPilotCompletion)
}
