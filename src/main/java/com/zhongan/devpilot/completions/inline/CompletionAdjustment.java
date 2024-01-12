package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.requests.AutocompleteRequest;
import com.zhongan.devpilot.completions.requests.AutocompleteResponse;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;

public abstract class CompletionAdjustment {
    public abstract SuggestionTrigger getSuggestionTrigger();
    private boolean cachedOnly = false;

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
