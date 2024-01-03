package com.zhongan.devpilot.common.capabilities

import com.intellij.openapi.util.registry.Registry
import com.zhongan.devpilot.completions.config.Config

class SuggestionsModeService {
    fun getSuggestionMode(): SuggestionsMode {
        if (Config.IS_SELF_HOSTED) {
            return SuggestionsMode.INLINE
        }

        val jbPreviewOn = Registry.`is`(
            "ide.lookup.preview.insertion"
        ) // If true, jetbrains build in preview feature is on

        if (jbPreviewOn) {
            return SuggestionsMode.AUTOCOMPLETE
        }

        if (CapabilitiesService.getInstance().isCapabilityEnabled(
                Capability.USE_HYBRID_INLINE_POPUP
            )
        ) {
            return SuggestionsMode.HYBRID
        }

        if (CapabilitiesService.getInstance().isCapabilityEnabled(
                Capability.INLINE_SUGGESTIONS
            )
        ) {
            return SuggestionsMode.INLINE
        }

        return SuggestionsMode.AUTOCOMPLETE
    }
}
