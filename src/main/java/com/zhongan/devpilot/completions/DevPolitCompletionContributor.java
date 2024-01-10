package com.zhongan.devpilot.completions;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.zhongan.devpilot.completions.common.capabilities.SuggestionsModeService;
import com.zhongan.devpilot.completions.common.general.DependencyContainer;
import com.zhongan.devpilot.completions.common.general.EditorUtils;
import com.zhongan.devpilot.completions.common.inline.DevPolitInlineLookupListener;
import org.jetbrains.annotations.NotNull;

public class DevPolitCompletionContributor extends CompletionContributor {
    private final DevPolitInlineLookupListener devpolitInlineLookupListener =
            DependencyContainer.instanceOfDevPolitInlineLookupListener();
    private final SuggestionsModeService suggestionsModeService =
            DependencyContainer.instanceOfSuggestionsModeService();

    @Override
    public void fillCompletionVariants(
            @NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        if (!EditorUtils.isMainEditor(parameters.getEditor())) {
            return;
        }
        if (suggestionsModeService.getSuggestionMode().isInlineEnabled()) {
            registerLookupListener(parameters, devpolitInlineLookupListener);
        }
        //select do not register
    }


    private void registerLookupListener(
            CompletionParameters parameters, LookupListener lookupListener) {
        final LookupEx lookupEx = LookupManager.getActiveLookup(parameters.getEditor());
        if (lookupEx == null) {
            return;
        }
        lookupEx.removeLookupListener(lookupListener);
        lookupEx.addLookupListener(lookupListener);
    }

}
