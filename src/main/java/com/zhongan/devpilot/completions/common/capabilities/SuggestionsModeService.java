package com.zhongan.devpilot.completions.common.capabilities;

public class SuggestionsModeService {
    public SuggestionsMode getSuggestionMode() {
        if (CapabilitiesService.getInstance().isCapabilityEnabled(Capability.INLINE_SUGGESTIONS)) {
            return SuggestionsMode.INLINE;
        }

        // val jbPreviewOn = Registry.`is`("ide.lookup.preview.insertion"); // If true, jetbrains build in preview feature is on
        // if (jbPreviewOn) {
        //     return SuggestionsMode.AUTOCOMPLETE;
        // }

        // if (CapabilitiesService.getInstance().isCapabilityEnabled(Capability.USE_HYBRID_INLINE_POPUP)) {
        //     return SuggestionsMode.HYBRID;
        // }

        return SuggestionsMode.AUTOCOMPLETE;
    }
}
