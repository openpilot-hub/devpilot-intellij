package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.util.ObjectUtils;
import com.zhongan.devpilot.completions.requests.AutocompleteResponse;
import com.zhongan.devpilot.completions.general.CompletionKind;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.inline.render.GraphicsUtilsKt;
import com.zhongan.devpilot.completions.completions.CompletionUtils;
import com.zhongan.devpilot.completions.prediction.CompletionFacade;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.zhongan.devpilot.completions.general.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InlineCompletionHandler {
    private final CompletionFacade completionFacade;
    private Future<?> lastDebounceRenderTask = null;
    private Future<?> lastFetchAndRenderTask = null;
    private Future<?> lastFetchInBackgroundTask = null;

    public InlineCompletionHandler(
            CompletionFacade completionFacade
    ) {
        this.completionFacade = completionFacade;
    }

    public void retrieveAndShowCompletion(
            @NotNull Editor editor,
            int offset,
            @Nullable DevPilotCompletion lastShownSuggestion,
            @NotNull String userInput,
            @NotNull CompletionAdjustment completionAdjustment) {
        Integer tabSize = GraphicsUtilsKt.getTabSize(editor);

        ObjectUtils.doIfNotNull(lastFetchInBackgroundTask, task -> task.cancel(false));
        ObjectUtils.doIfNotNull(lastFetchAndRenderTask, task -> task.cancel(false));
        ObjectUtils.doIfNotNull(lastDebounceRenderTask, task -> task.cancel(false));

        List<DevPilotCompletion> cachedCompletions =
                InlineCompletionCache.getInstance().retrieveAdjustedCompletions(editor, userInput);
        if (!cachedCompletions.isEmpty()) {
            renderCachedCompletions(editor, offset, tabSize, cachedCompletions, completionAdjustment);
            return;
        }

        ApplicationManager.getApplication()
                .invokeLater(
                        () ->
                                renderNewCompletions(
                                        editor,
                                        tabSize,
                                        getCurrentEditorOffset(editor, userInput),
                                        editor.getDocument().getModificationStamp(),
                                        completionAdjustment));
    }

    private void renderCachedCompletions(
            @NotNull Editor editor,
            int offset,
            Integer tabSize,
            @NotNull List<DevPilotCompletion> cachedCompletions,
            @NotNull CompletionAdjustment completionAdjustment) {
        showInlineCompletion(editor, cachedCompletions, offset, null);
        lastFetchInBackgroundTask =
                Utils.executeThread(
                        () -> retrieveInlineCompletion(editor, offset, tabSize, completionAdjustment));
    }

    private int getCurrentEditorOffset(@NotNull Editor editor, @NotNull String userInput) {
        return editor.getCaretModel().getOffset()
                + (ApplicationManager.getApplication().isUnitTestMode() ? userInput.length() : 0);
    }

    private void renderNewCompletions(
            @NotNull Editor editor,
            Integer tabSize,
            int offset,
            long modificationStamp,
            @NotNull CompletionAdjustment completionAdjustment) {
        lastFetchAndRenderTask =
                Utils.executeThread(
                        () -> {
                            CompletionTracker.updateLastCompletionRequestTime(editor);
                            List<DevPilotCompletion> beforeDebounceCompletions =
                                    retrieveInlineCompletion(editor, offset, tabSize, completionAdjustment);
                            long debounceTimeMs =
                                    beforeDebounceCompletions.isEmpty()
                                            ? logAndGetEmptySuggestionsDebounceMillis()
                                            : CompletionTracker.calcDebounceTimeMs(editor, completionAdjustment);

                            if (debounceTimeMs == 0) {
                                rerenderCompletion(
                                        editor,
                                        beforeDebounceCompletions,
                                        offset,
                                        modificationStamp,
                                        completionAdjustment);
                                return;
                            }

                            refetchCompletionsAfterDebounce(
                                    editor, tabSize, offset, modificationStamp, completionAdjustment, debounceTimeMs);
                        });
    }

    private int logAndGetEmptySuggestionsDebounceMillis() {
        int debounceMillis = 800;
        Logger.getInstance(getClass())
                .info(
                        String.format(
                                "Got empty suggestions, waiting %s ms before retrying to fetch", debounceMillis));
        return debounceMillis;
    }

    private void refetchCompletionsAfterDebounce(
            @NotNull Editor editor,
            Integer tabSize,
            int offset,
            long modificationStamp,
            @NotNull CompletionAdjustment completionAdjustment,
            long debounceTime) {
        lastDebounceRenderTask =
                Utils.executeThread(
                        () -> {
                            CompletionAdjustment cachedOnlyCompletionAdjustment =
                                    completionAdjustment.withCachedOnly();
                            List<DevPilotCompletion> completions =
                                    retrieveInlineCompletion(editor, offset, tabSize, cachedOnlyCompletionAdjustment);
                            rerenderCompletion(
                                    editor, completions, offset, modificationStamp, cachedOnlyCompletionAdjustment);
                        },
                        debounceTime,
                        TimeUnit.MILLISECONDS);
    }

    private void rerenderCompletion(
            @NotNull Editor editor,
            List<DevPilotCompletion> completions,
            int offset,
            long modificationStamp,
            @NotNull CompletionAdjustment completionAdjustment) {
        ApplicationManager.getApplication()
                .invokeLater(
                        () -> {
                            if (shouldCancelRendering(editor, modificationStamp, offset)) {
                                return;
                            }
/*                            if (shouldRemovePopupCompletions(completionAdjustment)) {
                                completions.removeIf(completion -> !completion.isSnippet());
                            }*/
                            showInlineCompletion(
                                    editor,
                                    completions,
                                    offset,
                                    (completion) -> afterCompletionShown(completion, editor));
                        });
    }

    private boolean shouldCancelRendering(
            @NotNull Editor editor, long modificationStamp, int offset) {
        if (Utils.isUnitTestMode()) {
            return false;
        }
        boolean isModificationStampChanged =
                modificationStamp != editor.getDocument().getModificationStamp();
        boolean isOffsetChanged = offset != editor.getCaretModel().getOffset();
        return isModificationStampChanged || isOffsetChanged;
    }

    /**
     * remove popup completions when 1. the suggestion mode is HYBRID and 2. the completion adjustment
     * type is not LookAhead
     */
/*    private boolean shouldRemovePopupCompletions(@NotNull CompletionAdjustment completionAdjustment) {
        return suggestionsModeService.getSuggestionMode() != SuggestionsMode.INLINE
                && completionAdjustment.getSuggestionTrigger() != SuggestionTrigger.LookAhead;
    }*/

    private List<DevPilotCompletion> retrieveInlineCompletion(
            @NotNull Editor editor,
            int offset,
            Integer tabSize,
            @NotNull CompletionAdjustment completionAdjustment) {
        AutocompleteResponse completionsResponse =
                this.completionFacade.retrieveCompletions(editor, offset, tabSize, completionAdjustment);

        if (completionsResponse == null || completionsResponse.results.length == 0) {
            return Collections.emptyList();
        }

        return createCompletions(
                completionsResponse,
                editor.getDocument(),
                offset,
                completionAdjustment.getSuggestionTrigger());
    }

    private void showInlineCompletion(
            @NotNull Editor editor,
            List<DevPilotCompletion> completions,
            int offset,
            @Nullable OnCompletionPreviewUpdatedCallback onCompletionPreviewUpdatedCallback) {
        if (completions.isEmpty()) {
            return;
        }
        InlineCompletionCache.getInstance().store(editor, completions);

        DevPilotCompletion displayedCompletion =
                CompletionPreview.createInstance(editor, completions, offset);

        if (displayedCompletion == null) {
            return;
        }

        if (onCompletionPreviewUpdatedCallback != null) {
            onCompletionPreviewUpdatedCallback.onCompletionPreviewUpdated(displayedCompletion);
        }
    }

    private void afterCompletionShown(DevPilotCompletion completion, Editor editor) {
        if (completion.completionMetadata == null) return;
        Boolean isCached = completion.completionMetadata.is_cached();

        try {
            String filename =
                    CompletionFacade.getFilename(FileDocumentManager.getInstance().getFile(editor.getDocument()));
            if (filename == null) {
                Logger.getInstance(getClass())
                        .warn("Could not send SuggestionShown request. the filename is null");
                return;
            }
//      this.binaryRequestFacade.executeRequest(
//          new SuggestionShownRequest(
//              completion.getNetLength(), filename, completion.completionMetadata));
            //TODO 调用openAI

            if (completion.completionMetadata.getCompletion_kind() == CompletionKind.Snippet
                    && !isCached) {
                Map<String, Object> context = completion.completionMetadata.getSnippet_context();
                if (context == null) {
                    Logger.getInstance(getClass())
                            .warn("Could not send SnippetShown request. intent is null");
                    return;
                }

//        this.binaryRequestFacade.executeRequest(new SnippetShownRequest(filename, context));
                //TODO 调用openAI
            }
        } catch (RuntimeException e) {
            // swallow - nothing to do with this
        }
    }

    private List<DevPilotCompletion> createCompletions(
            AutocompleteResponse completions,
            @NotNull Document document,
            int offset,
            SuggestionTrigger suggestionTrigger) {
        return IntStream.range(0, completions.results.length)
                .mapToObj(
                        index ->
                                CompletionUtils.createDevpilotCompletion(
                                        document,
                                        offset,
                                        completions.old_prefix,
                                        completions.results[index],
                                        index,
                                        suggestionTrigger))
                .filter(completion -> completion != null && !completion.getSuffix().isEmpty())
                .collect(Collectors.toList());
    }
}
