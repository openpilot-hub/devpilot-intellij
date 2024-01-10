package com.zhongan.devpilot.completions.requests

import com.zhongan.devpilot.completions.general.CompletionKind

data class CompletionMetadata(
//        val origin: CompletionOrigin? = null,
        val detail: String? = null,
        val completion_kind: CompletionKind? = null,
        val snippet_context: Map<String, Any>? = null,
        val is_cached: Boolean? = null,
        val deprecated: Boolean? = null,
) {
    fun getIsDeprecated(): Boolean {
        return deprecated ?: false
    }
}
