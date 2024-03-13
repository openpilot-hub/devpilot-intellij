//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.requests.AutocompleteRequest;
import com.zhongan.devpilot.completions.requests.AutocompleteResponse;

public abstract class CompletionAdjustment {
    private boolean cachedOnly = false;

    public CompletionAdjustment() {
    }

    public abstract SuggestionTrigger getSuggestionTrigger();

    public CompletionAdjustment withCachedOnly() {
        this.cachedOnly = true;
        return this;
    }

    public AutocompleteRequest adjustRequest(AutocompleteRequest request) {
        request.setCachedOnly(this.cachedOnly);
        return this.adjustRequestInner(request);
    }

    protected AutocompleteRequest adjustRequestInner(AutocompleteRequest autocompleteRequest) {
        return autocompleteRequest;
    }

    public AutocompleteResponse adjustResponse(AutocompleteResponse autocompleteResponse) {
        return autocompleteResponse;
    }
}
