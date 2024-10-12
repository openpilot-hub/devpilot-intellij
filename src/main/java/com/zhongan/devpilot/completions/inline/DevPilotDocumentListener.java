package com.zhongan.devpilot.completions.inline;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.wm.IdeFocusManager;
import com.zhongan.devpilot.completions.general.EditorUtils;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;

import java.awt.Component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.editor.EditorModificationUtil.checkModificationAllowed;
import static com.zhongan.devpilot.completions.general.DependencyContainer.singletonOfInlineCompletionHandler;

public class DevPilotDocumentListener implements BulkAwareDocumentListener {
    private final InlineCompletionHandler handler = singletonOfInlineCompletionHandler();

    @Nullable
    private static Editor getActiveEditor(@NotNull Document document) {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            return null;
        }

        Component focusOwner = IdeFocusManager.getGlobalInstance().getFocusOwner();
        DataContext dataContext = DataManager.getInstance().getDataContext(focusOwner);
        // ignore caret placing when exiting
        Editor activeEditor =
                ApplicationManager.getApplication().isDisposed()
                        ? null
                        : CommonDataKeys.EDITOR.getData(dataContext);

        if (activeEditor != null && activeEditor.getDocument() != document) {
            activeEditor = null;
        }

        return activeEditor;
    }

    @Override
    public void documentChangedNonBulk(@NotNull DocumentEvent event) {
        if (!CompletionSettingsState.getInstance().getEnable()) {
            return;
        }
        Document document = event.getDocument();
        Editor editor = getActiveEditor(document);
        if (editor == null || !EditorUtils.isMainEditor(editor)) {
            return;
        }
        DevPilotCompletion lastShownCompletion = CompletionPreview.getCurrentCompletion(editor);
        CompletionPreview completionPreview = CompletionPreview.getInstance(editor);

        if (completionPreview != null && completionPreview.isByLineAcceptDocumentChange(event)) {
            return;
        }
        CompletionPreview.clear(editor);
        int offset = event.getOffset() + event.getNewLength();

        CompletionUtils.VerifyResult result = shouldIgnoreChange(event, editor, offset, lastShownCompletion);

        if (result.isValid()) {
            InlineCompletionCache.INSTANCE.clear(editor);
            return;
        }

        if (!CompletionUtils.checkTriggerTime(editor,
                offset,
                lastShownCompletion,
                event.getNewFragment().toString(),
                new DefaultCompletionAdjustment(),
                result.getCompletionType())) {
            return;
        }
        handler.retrieveAndShowCompletion(
                editor,
                offset,
                lastShownCompletion,
                event.getNewFragment().toString(),
                new DefaultCompletionAdjustment(),
                result.getCompletionType());

    }

    private CompletionUtils.VerifyResult shouldIgnoreChange(
            DocumentEvent event, Editor editor, int offset, DevPilotCompletion lastShownCompletion) {
        Document document = event.getDocument();

//        if (!suggestionsModeService.getSuggestionMode().isInlineEnabled()) {
//            return true;
//        }

        if (event.getNewLength() < 1) {
            return CompletionUtils.VerifyResult.create(true);
        }

        if (!editor.getEditorKind().equals(EditorKind.MAIN_EDITOR)
                && !ApplicationManager.getApplication().isUnitTestMode()) {
            return CompletionUtils.VerifyResult.create(true);
        }

        if (!checkModificationAllowed(editor) || document.getRangeGuard(offset, offset) != null) {
            document.fireReadOnlyModificationAttempt();
            return CompletionUtils.VerifyResult.create(true);
        }

//        return !CompletionUtils.isValidDocumentChange(document, offset, event.getOffset());

        CompletionUtils.VerifyResult result = CompletionUtils
                .isValidChange(editor, document, offset, event.getOffset());
        return CompletionUtils.VerifyResult.create(!result.isValid(), result.getCompletionType());
    }

}
