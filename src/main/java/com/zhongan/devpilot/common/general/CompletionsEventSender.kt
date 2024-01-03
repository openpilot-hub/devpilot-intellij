package com.zhongan.devpilot.common.general

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.zhongan.devpilot.common.binary.BinaryRequest
//import com.zhongan.devpilot.common.binary.BinaryRequestFacade
import com.zhongan.devpilot.common.binary.BinaryResponse
import com.zhongan.devpilot.common.binary.requests.analytics.EventRequest
import com.zhongan.devpilot.common.binary.requests.notifications.shown.SuggestionDroppedReason
import com.zhongan.devpilot.common.binary.requests.notifications.shown.SuggestionDroppedRequest
import com.zhongan.devpilot.common.capabilities.RenderingMode
import com.zhongan.devpilot.common.inline.CompletionOrder
import com.zhongan.devpilot.common.prediction.CompletionFacade.getFilename
import com.zhongan.devpilot.common.prediction.DevPilotCompletion

//class CompletionsEventSender(private val binaryRequestFacade: BinaryRequestFacade) {
class CompletionsEventSender() {
    fun sendToggleInlineSuggestionEvent(order: CompletionOrder, index: Int) {
        val eventOrder = if (order == CompletionOrder.NEXT) {
            "next"
        } else {
            "previous"
        }
        val eventName = "toggle-$eventOrder-suggestion"
        val event = EventRequest(eventName, mapOf("suggestion_index" to index.toString()))

        sendEventAsync(event)
    }

    fun sendManualSuggestionTrigger(renderingMode: RenderingMode) {
        val event = EventRequest("manual-suggestion-trigger", mapOf("suggestion_rendering_mode" to renderingMode.name))
        sendEventAsync(event)
    }

    fun sendSuggestionDropped(editor: Editor, suggestion: DevPilotCompletion?, reason: SuggestionDroppedReason) {
        if (suggestion == null) return

        try {
            val filename = getFilename(FileDocumentManager.getInstance().getFile(editor.document))
            if (filename == null) {
                Logger.getInstance(javaClass).warn("Failed to obtain filename, skipping sending suggestion dropped with reason = $reason")
                return
            }
            val netLength = suggestion.netLength
            val metadata = suggestion.completionMetadata

            val event = SuggestionDroppedRequest(netLength, reason, filename, metadata)
            sendEventAsync(event)
        } catch (t: Throwable) {
            Logger.getInstance(javaClass).warn("Failed to send suggestion dropped with reason = $reason", t)
        }
    }

    private fun <R : BinaryResponse> sendEventAsync(event: BinaryRequest<R>) {
        AppExecutorUtil.getAppExecutorService().submit {
            //TODO 调用openai
//            binaryRequestFacade.executeRequest(event)
        }
    }
}
