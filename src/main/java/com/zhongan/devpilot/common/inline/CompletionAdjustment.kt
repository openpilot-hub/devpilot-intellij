package com.zhongan.devpilot.common.inline

import com.zhongan.devpilot.common.binary.requests.autocomplete.AutocompleteRequest
import com.zhongan.devpilot.common.binary.requests.autocomplete.AutocompleteResponse
import com.zhongan.devpilot.common.general.SuggestionTrigger

abstract class CompletionAdjustment {
    abstract val suggestionTrigger: SuggestionTrigger
    var cachedOnly: Boolean = false

    fun withCachedOnly(): CompletionAdjustment {
        cachedOnly = true
        return this
    }

    fun adjustRequest(request: AutocompleteRequest): AutocompleteRequest {
        request.cached_only = cachedOnly
        return adjustRequestInner(request)
    }

    protected open fun adjustRequestInner(autocompleteRequest: AutocompleteRequest): AutocompleteRequest = autocompleteRequest
    open fun adjustResponse(autocompleteResponse: AutocompleteResponse): AutocompleteResponse = autocompleteResponse
}
