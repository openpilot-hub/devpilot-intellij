//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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

    public InlineCompletionHandler(CompletionFacade completionFacade) {
        this.completionFacade = completionFacade;
    }

    public void retrieveAndShowCompletion(@NotNull Editor editor, int offset, @Nullable DevPilotCompletion lastShownSuggestion, @NotNull String userInput, @NotNull CompletionAdjustment completionAdjustment) {

        Integer tabSize = GraphicsUtils.getTabSize(editor);
        ObjectUtils.doIfNotNull(this.lastFetchInBackgroundTask, (task) -> {
            return task.cancel(false);
        });
        ObjectUtils.doIfNotNull(this.lastFetchAndRenderTask, (task) -> {
            return task.cancel(false);
        });
        ObjectUtils.doIfNotNull(this.lastDebounceRenderTask, (task) -> {
            return task.cancel(false);
        });
        List<DevPilotCompletion> cachedCompletions = InlineCompletionCache.INSTANCE.retrieveAdjustedCompletions(editor, userInput);
        if (!cachedCompletions.isEmpty()) {
            this.renderCachedCompletions(editor, offset, tabSize, cachedCompletions, completionAdjustment);
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                this.renderNewCompletions(editor, tabSize, this.getCurrentEditorOffset(editor, userInput), editor.getDocument().getModificationStamp(), completionAdjustment);
            });
        }
    }

    private void renderCachedCompletions(@NotNull Editor editor, int offset, Integer tabSize, @NotNull List<DevPilotCompletion> cachedCompletions, @NotNull CompletionAdjustment completionAdjustment) {

        this.showInlineCompletion(editor, cachedCompletions, offset, (OnCompletionPreviewUpdatedCallback)null);
        this.lastFetchInBackgroundTask = Utils.executeThread(() -> {
            this.retrieveInlineCompletion(editor, offset, tabSize, completionAdjustment);
        });
    }

    private int getCurrentEditorOffset(@NotNull Editor editor, @NotNull String userInput) {

        return editor.getCaretModel().getOffset() + (ApplicationManager.getApplication().isUnitTestMode() ? userInput.length() : 0);
    }

    private void renderNewCompletions(@NotNull Editor editor, Integer tabSize, int offset, long modificationStamp, @NotNull CompletionAdjustment completionAdjustment) {

        this.lastFetchAndRenderTask = Utils.executeThread(() -> {
            CompletionTracker.updateLastCompletionRequestTime(editor);
            long debounceTimeMs = CompletionTracker.calcDebounceTimeMs(editor, completionAdjustment);
            if (debounceTimeMs == 0L) {
                debounceTimeMs = (long)this.logAndGetEmptySuggestionsDebounceMillis();
            }

            this.refetchCompletionsAfterDebounce(editor, tabSize, offset, modificationStamp, completionAdjustment, debounceTimeMs);
        });
    }

    private int logAndGetEmptySuggestionsDebounceMillis() {
        int debounceMillis = 300;
        Logger.getInstance(this.getClass()).info(String.format("Got empty suggestions, waiting %s ms before retrying to fetch", Integer.valueOf(debounceMillis)));
        return debounceMillis;
    }

    private void refetchCompletionsAfterDebounce(@NotNull Editor editor, Integer tabSize, int offset, long modificationStamp, @NotNull CompletionAdjustment completionAdjustment, long debounceTime) {


        this.lastDebounceRenderTask = Utils.executeThread(() -> {
            CompletionAdjustment cachedOnlyCompletionAdjustment = completionAdjustment.withCachedOnly();
            List<DevPilotCompletion> completions = this.retrieveInlineCompletion(editor, offset, tabSize, cachedOnlyCompletionAdjustment);
            this.rerenderCompletion(editor, completions, offset, modificationStamp, cachedOnlyCompletionAdjustment);
        }, debounceTime, TimeUnit.MILLISECONDS);
    }

    private void rerenderCompletion(@NotNull Editor editor, List<DevPilotCompletion> completions, int offset, long modificationStamp, @NotNull CompletionAdjustment var6) {


        ApplicationManager.getApplication().invokeLater(() -> {
            if (!this.shouldCancelRendering(editor, modificationStamp, offset)) {
                this.showInlineCompletion(editor, completions, offset, (completion) -> {
                    this.afterCompletionShown(completion, editor);
                });
            }
        });
    }

    private boolean shouldCancelRendering(@NotNull Editor editor, long modificationStamp, int offset) {


        if (Utils.isUnitTestMode()) {
            return false;
        } else {
            boolean isModificationStampChanged = modificationStamp != editor.getDocument().getModificationStamp();
            boolean isOffsetChanged = offset != editor.getCaretModel().getOffset();
            return isModificationStampChanged || isOffsetChanged;
        }
    }

    private List<DevPilotCompletion> retrieveInlineCompletion(@NotNull Editor editor, int offset, Integer tabSize, @NotNull CompletionAdjustment completionAdjustment) {


        AutocompleteResponse completionsResponse = this.completionFacade.retrieveCompletions(editor, offset, tabSize, completionAdjustment);
        return completionsResponse != null && completionsResponse.results.length != 0 ? this.createCompletions(completionsResponse, editor.getDocument(), offset, completionAdjustment.getSuggestionTrigger()) : Collections.emptyList();
    }

    private void showInlineCompletion(@NotNull Editor editor, List<DevPilotCompletion> completions, int offset, @Nullable OnCompletionPreviewUpdatedCallback onCompletionPreviewUpdatedCallback) {


        if (!completions.isEmpty()) {
            InlineCompletionCache var10000 = InlineCompletionCache.INSTANCE;
            InlineCompletionCache.store(editor, completions);
            DevPilotCompletion displayedCompletion = CompletionPreview.createInstance(editor, completions, offset);
            if (displayedCompletion != null) {
                if (onCompletionPreviewUpdatedCallback != null) {
                    onCompletionPreviewUpdatedCallback.onCompletionPreviewUpdated(displayedCompletion);
                }

            }
        }
    }

    private void afterCompletionShown(DevPilotCompletion completion, Editor editor) {
        if (completion.completionMetadata != null) {
            Boolean isCached = completion.completionMetadata.getIsCached();

            try {
                String filename = CompletionFacade.getFilename(FileDocumentManager.getInstance().getFile(editor.getDocument()));
                if (filename == null) {
                    Logger.getInstance(this.getClass()).warn("Could not send SuggestionShown request. the filename is null");
                    return;
                }

                if (completion.completionMetadata.getCompletionKind() == CompletionKind.Snippet && !isCached) {
                    Map<String, Object> context = completion.completionMetadata.getSnippetContext();
                    if (context == null) {
                        Logger.getInstance(this.getClass()).warn("Could not send SnippetShown request. intent is null");
                        return;
                    }
                }
            } catch (RuntimeException var6) {
            }

        }
    }

    private List<DevPilotCompletion> createCompletions(AutocompleteResponse completions, @NotNull Document document, int offset, SuggestionTrigger suggestionTrigger) {


        return (List)IntStream.range(0, completions.results.length).mapToObj((index) -> {
            return CompletionUtils.createDevpilotCompletion(document, offset, completions.oldPrefix, completions.results[index], index, suggestionTrigger);
        }).filter((completion) -> {
            return completion != null && !completion.getSuffix().isEmpty();
        }).collect(Collectors.toList());
    }
}
