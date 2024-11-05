package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.util.ObjectUtils;
import com.zhongan.devpilot.completions.CompletionUtils;
import com.zhongan.devpilot.completions.general.CompletionKind;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.general.Utils;
import com.zhongan.devpilot.completions.inline.render.GraphicsUtils;
import com.zhongan.devpilot.completions.prediction.CompletionFacade;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.completions.requests.AutocompleteResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        @NotNull CompletionAdjustment completionAdjustment,
        String completionType) {
        Integer tabSize = GraphicsUtils.getTabSize(editor);

        ObjectUtils.doIfNotNull(lastFetchInBackgroundTask, task -> task.cancel(false));
        ObjectUtils.doIfNotNull(lastFetchAndRenderTask, task -> task.cancel(false));
        ObjectUtils.doIfNotNull(lastDebounceRenderTask, task -> task.cancel(false));

        List<DevPilotCompletion> cachedCompletions =
            InlineCompletionCache.INSTANCE.retrieveAdjustedCompletions(editor, userInput);
        if (!cachedCompletions.isEmpty()) {
            renderCachedCompletions(editor, offset, tabSize, cachedCompletions, completionAdjustment, completionType);
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
                        completionAdjustment,
                        completionType));
    }

    private void renderCachedCompletions(
        @NotNull Editor editor,
        int offset,
        Integer tabSize,
        @NotNull List<DevPilotCompletion> cachedCompletions,
        @NotNull CompletionAdjustment completionAdjustment,
        String completionType) {
        showInlineCompletion(editor, cachedCompletions, offset, null);
        lastFetchInBackgroundTask =
            Utils.executeThread(
                () -> retrieveInlineCompletion(editor, offset, tabSize, completionAdjustment, completionType));
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
        @NotNull CompletionAdjustment completionAdjustment,
        String completionType) {
        lastFetchAndRenderTask =
            Utils.executeThread(
                () -> {
                    CompletionTracker.updateLastCompletionRequestTime(editor);
                    long debounceTimeMs = CompletionTracker.calcDebounceTimeMs(editor, completionAdjustment);
                    if (debounceTimeMs == 0) {
                        debounceTimeMs = logAndGetEmptySuggestionsDebounceMillis();
                    }
                    refetchCompletionsAfterDebounce(
                        editor, tabSize, offset, modificationStamp, completionAdjustment, debounceTimeMs, completionType);
                });
    }

    private int logAndGetEmptySuggestionsDebounceMillis() {
        int debounceMillis = 300;
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
        long debounceTime,
        String completionType) {
        lastDebounceRenderTask =
            Utils.executeThread(
                () -> {
                    CompletionAdjustment cachedOnlyCompletionAdjustment =
                        completionAdjustment.withCachedOnly();
                    List<DevPilotCompletion> completions =
                        retrieveInlineCompletion(editor, offset, tabSize, cachedOnlyCompletionAdjustment, completionType);
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

    private List<DevPilotCompletion> retrieveInlineCompletion(
        @NotNull Editor editor,
        int offset,
        Integer tabSize,
        @NotNull CompletionAdjustment completionAdjustment,
        String completionType) {
        AutocompleteResponse completionsResponse =
            this.completionFacade.retrieveCompletions(editor, offset, tabSize, completionAdjustment, completionType);

        if (completionsResponse == null || completionsResponse.results.length == 0) {
            return Collections.emptyList();
        }

        return createCompletions(
            editor,
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
        InlineCompletionCache.INSTANCE.store(editor, completions);

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
        Boolean isCached = completion.completionMetadata.getIsCached();

        try {
            String filename =
                CompletionFacade.getFilename(FileDocumentManager.getInstance().getFile(editor.getDocument()));
            if (filename == null) {
                Logger.getInstance(getClass())
                    .warn("Could not send SuggestionShown request. the filename is null");
                return;
            }
            //TODO 调用openAI

            if (completion.completionMetadata.getCompletionKind() == CompletionKind.Snippet
                && !isCached) {
                Map<String, Object> context = completion.completionMetadata.getSnippetContext();
                if (context == null) {
                    Logger.getInstance(getClass())
                        .warn("Could not send SnippetShown request. intent is null");
                    return;
                }

                //TODO 调用openAI
            }
        } catch (RuntimeException e) {
            // swallow - nothing to do with this
        }
    }

    private List<DevPilotCompletion> createCompletions(
        @NotNull Editor editor,
        @NotNull
        AutocompleteResponse completions,
        @NotNull Document document,
        int offset,
        SuggestionTrigger suggestionTrigger) {
        return IntStream.range(0, completions.results.length)
            .mapToObj(
                index ->
                    CompletionUtils.createDevpilotCompletion(
                            editor,
                        document,
                        offset,
                        completions.oldPrefix,
                        completions.results[index],
                        index,
                        suggestionTrigger))
            .filter(completion -> completion != null && !completion.getSuffix().isEmpty())
            .collect(Collectors.toList());
    }
}
