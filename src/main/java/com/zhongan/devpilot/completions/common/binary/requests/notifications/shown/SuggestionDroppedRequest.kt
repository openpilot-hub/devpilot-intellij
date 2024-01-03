package com.zhongan.devpilot.completions.common.binary.requests.notifications.shown

import com.zhongan.devpilot.completions.common.binary.BinaryRequest
import com.zhongan.devpilot.completions.common.binary.requests.EmptyResponse
import com.zhongan.devpilot.completions.common.binary.requests.autocomplete.CompletionMetadata

enum class SuggestionDroppedReason {
    ManualCancel,
    ScrollLookAhead,
    TextDeletion,
    UserNotTypedAsSuggested,
    CaretMoved,
    FocusChanged,
}

data class SuggestionDroppedRequest(
    var net_length: Int,
    var reason: SuggestionDroppedReason? = null,
    var filename: String? = null,
    var metadata: CompletionMetadata? = null
) : BinaryRequest<EmptyResponse> {
    override fun response(): Class<EmptyResponse> {
        return EmptyResponse::class.java
    }
}
