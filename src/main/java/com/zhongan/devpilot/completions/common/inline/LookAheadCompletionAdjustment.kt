package com.zhongan.devpilot.completions.common.inline

import com.zhongan.devpilot.completions.common.binary.requests.autocomplete.AutocompleteRequest
import com.zhongan.devpilot.completions.common.binary.requests.autocomplete.AutocompleteResponse
import com.zhongan.devpilot.completions.common.binary.requests.autocomplete.ResultEntry
import com.zhongan.devpilot.completions.common.general.SuggestionTrigger
import java.util.Arrays

class LookAheadCompletionAdjustment(private val userPrefix: String, private val focusedCompletion: String) :
        CompletionAdjustment() {
    override fun adjustRequestInner(autocompleteRequest: AutocompleteRequest): AutocompleteRequest {
        autocompleteRequest.before = (
                autocompleteRequest.before.substring(
                        0, autocompleteRequest.before.length - userPrefix.length
                ) +
                        focusedCompletion
                )
        return autocompleteRequest
    }

    override fun adjustResponse(autocompleteResponse: AutocompleteResponse): AutocompleteResponse {
        autocompleteResponse.old_prefix = userPrefix
        autocompleteResponse.results = Arrays.stream(autocompleteResponse.results)
                .filter { resultEntry -> resultEntry.new_prefix.startsWith(focusedCompletion) }
                .toArray { size -> arrayOfNulls<ResultEntry>(size) }
        return autocompleteResponse
    }

    override val suggestionTrigger: SuggestionTrigger
        get() = SuggestionTrigger.LookAhead
}
