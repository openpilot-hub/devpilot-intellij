package com.zhongan.devpilot.completions.inline;

import static com.intellij.openapi.editor.EditorModificationUtil.checkModificationAllowed;
import static com.zhongan.devpilot.completions.general.DependencyContainer.singletonOfInlineCompletionHandler;
import static com.zhongan.devpilot.completions.general.StaticConfig.MIN_DELAY_TIME_IN_MILLIS;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.wm.IdeFocusManager;
import com.zhongan.devpilot.completions.general.EditorUtils;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DevPolitDocumentListener implements BulkAwareDocumentListener {
    private final InlineCompletionHandler handler = singletonOfInlineCompletionHandler();

    @Override
    public void documentChangedNonBulk(@NotNull DocumentEvent event) {
        //TODO Get from settings page
/*    if (!CompletionsState.INSTANCE.isCompletionsEnabled()) {
      return;
    }*/
        Document document = event.getDocument();
        Editor editor = getActiveEditor(document);
        if (editor == null || !EditorUtils.isMainEditor(editor)) {
            return;
        }
        DevPilotCompletion lastShownCompletion = CompletionPreview.getCurrentCompletion(editor);
        CompletionPreview.clear(editor);
        int offset = event.getOffset() + event.getNewLength();

        if (shouldIgnoreChange(event, editor, offset, lastShownCompletion)) {
            InlineCompletionCache.instance.clear(editor);
            return;
        }

        handler.retrieveAndShowCompletion(
                editor,
                offset,
                lastShownCompletion,
                event.getNewFragment().toString(),
                new DefaultCompletionAdjustment());

    }

    private boolean shouldIgnoreChange(
            DocumentEvent event, Editor editor, int offset, DevPilotCompletion lastShownCompletion) {
        Document document = event.getDocument();

/*        if (!suggestionsModeService.getSuggestionMode().isInlineEnabled()) {
            return true;
        }*/

        if (event.getNewLength() < 1) {
            return true;
        }

        if (!editor.getEditorKind().equals(EditorKind.MAIN_EDITOR)
                && !ApplicationManager.getApplication().isUnitTestMode()) {
            return true;
        }

        if (!checkModificationAllowed(editor) || document.getRangeGuard(offset, offset) != null) {
            document.fireReadOnlyModificationAttempt();

            return true;
        }

        return !CompletionUtils.isValidDocumentChange(document, offset, event.getOffset());
    }

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

}
