package com.zhongan.devpilot.common.inline

import com.zhongan.devpilot.common.prediction.DevPilotCompletion

fun hadSuffix(currentCompletion: DevPilotCompletion): Boolean {
    return currentCompletion.oldSuffix?.trim()?.isNotEmpty() ?: false
}

fun isSingleLine(currentCompletion: DevPilotCompletion): Boolean {
    return !currentCompletion.suffix.trim().contains("\n")
}
fun shouldRemoveSuffix(currentCompletion: DevPilotCompletion): Boolean {
    return hadSuffix(currentCompletion) && isSingleLine(currentCompletion)
}
