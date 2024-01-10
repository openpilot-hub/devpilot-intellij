package com.zhongan.devpilot.completions;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.diagnostic.Logger;
import com.zhongan.devpilot.completions.common.completions.Completion;
import com.zhongan.devpilot.completions.common.binary.requests.autocomplete.AutocompleteResponse;
import com.zhongan.devpilot.completions.common.binary.requests.autocomplete.ResultEntry;
import com.zhongan.devpilot.completions.common.capabilities.SuggestionsMode;
import com.zhongan.devpilot.completions.common.capabilities.SuggestionsModeService;
import com.zhongan.devpilot.completions.common.general.DependencyContainer;
import com.zhongan.devpilot.completions.common.general.EditorUtils;
import com.zhongan.devpilot.completions.common.general.StaticConfig;
import com.zhongan.devpilot.completions.common.inline.DevPolitInlineLookupListener;
import com.zhongan.devpilot.completions.common.inline.render.GraphicsUtilsKt;
import com.zhongan.devpilot.completions.common.completions.CompletionUtils;
import com.zhongan.devpilot.completions.common.prediction.CompletionFacade;
import com.zhongan.devpilot.completions.common.prediction.DevPilotCompletion;
import com.zhongan.devpilot.completions.prediction.DevPilotWeigher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

import static com.zhongan.devpilot.completions.common.general.StaticConfig.*;

public class DevPolitCompletionContributor extends CompletionContributor {
    private final CompletionFacade completionFacade =
            DependencyContainer.instanceOfCompletionFacade();

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

        if (!parameters.isAutoPopup()) {
//      completionsEventSender.sendManualSuggestionTrigger(RenderingMode.AUTOCOMPLETE);
        }

        if (suggestionsModeService.getSuggestionMode().isInlineEnabled()) {
            registerLookupListener(parameters, devpolitInlineLookupListener);
        }
        if (!suggestionsModeService.getSuggestionMode().isPopupEnabled()) {
            return;
        }
        //select do not register
//    registerLookupListener(parameters, devPolitLookupListener);
        //TODO 调用openai gayway
        AutocompleteResponse completions =
                this.completionFacade.retrieveCompletions(
                        parameters, GraphicsUtilsKt.getTabSize(parameters.getEditor()));

        if (completions == null) {
            return;
        }

        PrefixMatcher originalMatcher = resultSet.getPrefixMatcher();

        if (originalMatcher.getPrefix().length() == 0 && completions.results.length == 0) {
            return;
        }

        if (suggestionsModeService.getSuggestionMode() == SuggestionsMode.HYBRID
                && Arrays.stream(completions.results).anyMatch(Completion::isSnippet)) {
            return;
        }

        resultSet =
                resultSet
                        .withPrefixMatcher(
                                new DevPolitPrefixMatcher(originalMatcher.cloneWithPrefix(completions.old_prefix)))
                        .withRelevanceSorter(
                                CompletionSorter.defaultSorter(parameters, originalMatcher)
                                        .weigh(new DevPilotWeigher()));
        resultSet.restartCompletionOnAnyPrefixChange();
        addAdvertisement(resultSet, completions);

        resultSet.addAllElements(createCompletions(completions, parameters, resultSet));
    }

    private ArrayList<LookupElement> createCompletions(
            AutocompleteResponse completions,
            @NotNull CompletionParameters parameters,
            @NotNull CompletionResultSet resultSet) {
        ArrayList<LookupElement> elements = new ArrayList<>();
        final Lookup activeLookup = LookupManager.getActiveLookup(parameters.getEditor());
        for (int index = 0;
             index < completions.results.length
                     && index
                     < CompletionUtils.completionLimit(parameters, resultSet, completions.is_locked);
             index++) {
            LookupElement lookupElement =
                    createCompletion(
                            parameters,
                            completions.old_prefix,
                            completions.results[index],
                            index,
                            completions.is_locked,
                            activeLookup);

            if (lookupElement != null && resultSet.getPrefixMatcher().prefixMatches(lookupElement)) {
                elements.add(lookupElement);
            }
        }

        return elements;
    }

    @Nullable
    private LookupElement createCompletion(
            CompletionParameters parameters,
            String oldPrefix,
            ResultEntry result,
            int index,
            boolean locked,
            @Nullable Lookup activeLookup) {
        DevPilotCompletion completion =
                CompletionUtils.createDevpilotCompletion(
                        parameters.getEditor().getDocument(),
                        parameters.getOffset(),
                        oldPrefix,
                        result,
                        index,
                        null);
        if (completion == null) {
            return null;
        }

        LookupElementBuilder lookupElementBuilder =
                LookupElementBuilder.create(completion, result.new_prefix)
                        .withRenderer(
                                new LookupElementRenderer<LookupElement>() {
                                    @Override
                                    public void renderElement(
                                            LookupElement element, LookupElementPresentation presentation) {
                                        DevPilotCompletion lookupElement = (DevPilotCompletion) element.getObject();
                                        presentation.setTypeText(StaticConfig.BRAND_NAME);
                                        presentation.setItemTextBold(false);
                                        presentation.setStrikeout(
                                                lookupElement.completionMetadata != null
                                                        && lookupElement.completionMetadata.getIsDeprecated());
                                        presentation.setItemText(lookupElement.newPrefix);
                                        presentation.setIcon(getDevpilotIcon());
                                    }
                                });
        if (!locked) {
            lookupElementBuilder =
                    lookupElementBuilder.withInsertHandler(
                            (context, item) -> {
                                int end = context.getTailOffset();
                                DevPilotCompletion lookupElement = (DevPilotCompletion) item.getObject();
                                try {
                                    context
                                            .getDocument()
                                            .insertString(
                                                    end + lookupElement.oldSuffix.length(), lookupElement.newSuffix);
                                    context.getDocument().deleteString(end, end + lookupElement.oldSuffix.length());
                                } catch (RuntimeException re) {
                                    Logger.getInstance(getClass())
                                            .warn(
                                                    "Error inserting new suffix. End = "
                                                            + end
                                                            + ", old suffix length = "
                                                            + lookupElement.oldSuffix.length()
                                                            + ", new suffix length = "
                                                            + lookupElement.newSuffix.length(),
                                                    re);
                                }
                            });
        }
        return lookupElementBuilder;
    }

    private void addAdvertisement(
            @NotNull CompletionResultSet result, AutocompleteResponse completions) {
        if (completions.user_message.length >= 1) {
            String details = String.join(" ", completions.user_message);

            details = details.substring(0, Math.min(details.length(), ADVERTISEMENT_MAX_LENGTH));

            result.addLookupAdvertisement(details);
        }
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
