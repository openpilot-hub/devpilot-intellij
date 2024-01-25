package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.requests.AutocompleteRequest;
import com.zhongan.devpilot.completions.requests.AutocompleteResponse;

public abstract class CompletionAdjustment {
    private boolean cachedOnly = false;

    public abstract SuggestionTrigger getSuggestionTrigger();

    public CompletionAdjustment withCachedOnly() {
        cachedOnly = true;
        return this;
    }

    public AutocompleteRequest adjustRequest(AutocompleteRequest request) {
        request.setCachedOnly(cachedOnly);
        return adjustRequestInner(request);
    }

    protected AutocompleteRequest adjustRequestInner(AutocompleteRequest autocompleteRequest) {
        return autocompleteRequest;
    }

    public AutocompleteResponse adjustResponse(AutocompleteResponse autocompleteResponse) {
        return autocompleteResponse;
    }
}
