package com.zhongan.devpilot.completions.common.binary.requests.analytics

import com.zhongan.devpilot.completions.common.binary.BinaryRequest
import com.zhongan.devpilot.completions.common.binary.requests.EmptyResponse

data class EventRequest(var name: String, var properties: Map<String, String>?) :
        BinaryRequest<EmptyResponse> {
    override fun response(): Class<EmptyResponse> {
        return EmptyResponse::class.java
    }

}
