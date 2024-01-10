package com.zhongan.devpilot.completions;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.zhongan.devpilot.completions.general.DependencyContainer;
import com.zhongan.devpilot.completions.general.EditorUtils;
import com.zhongan.devpilot.completions.inline.DevPolitInlineLookupListener;
import org.jetbrains.annotations.NotNull;

public class DevPolitInlineCompletionContributor extends CompletionContributor {
    private final DevPolitInlineLookupListener devpolitInlineLookupListener =
            DependencyContainer.instanceOfDevPolitInlineLookupListener();

    @Override
    public void fillCompletionVariants(
            @NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        if (!EditorUtils.isMainEditor(parameters.getEditor())) {
            return;
        }
        registerLookupListener(parameters, devpolitInlineLookupListener);
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
