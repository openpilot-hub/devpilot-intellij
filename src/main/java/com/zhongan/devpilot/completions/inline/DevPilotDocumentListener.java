//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.wm.IdeFocusManager;
import com.zhongan.devpilot.completions.general.DependencyContainer;
import com.zhongan.devpilot.completions.general.EditorUtils;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import java.awt.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DevPilotDocumentListener implements BulkAwareDocumentListener {
    private final InlineCompletionHandler handler = DependencyContainer.singletonOfInlineCompletionHandler();

    public DevPilotDocumentListener() {
    }

    private static @Nullable Editor getActiveEditor(@NotNull Document document) {


        if (!ApplicationManager.getApplication().isDispatchThread()) {
            return null;
        } else {
            Component focusOwner = IdeFocusManager.getGlobalInstance().getFocusOwner();
            DataContext dataContext = DataManager.getInstance().getDataContext(focusOwner);
            Editor activeEditor = ApplicationManager.getApplication().isDisposed() ? null : (Editor)CommonDataKeys.EDITOR.getData(dataContext);
            if (activeEditor != null && activeEditor.getDocument() != document) {
                activeEditor = null;
            }

            return activeEditor;
        }
    }

    public void documentChangedNonBulk(@NotNull DocumentEvent event) {


        if (CompletionSettingsState.getInstance().getEnable()) {
            Document document = event.getDocument();
            Editor editor = getActiveEditor(document);
            if (editor != null && EditorUtils.isMainEditor(editor)) {
                DevPilotCompletion lastShownCompletion = CompletionPreview.getCurrentCompletion(editor);
                CompletionPreview.clear(editor);
                int offset = event.getOffset() + event.getNewLength();
                if (this.shouldIgnoreChange(event, editor, offset, lastShownCompletion)) {
                    InlineCompletionCache var10000 = InlineCompletionCache.INSTANCE;
                    InlineCompletionCache.clear(editor);
                } else {
                    this.handler.retrieveAndShowCompletion(editor, offset, lastShownCompletion, event.getNewFragment().toString(), new DefaultCompletionAdjustment());
                }
            }
        }
    }

    private boolean shouldIgnoreChange(DocumentEvent event, Editor editor, int offset, DevPilotCompletion lastShownCompletion) {
        Document document = event.getDocument();
        if (event.getNewLength() < 1) {
            return true;
        } else if (!editor.getEditorKind().equals(EditorKind.MAIN_EDITOR) && !ApplicationManager.getApplication().isUnitTestMode()) {
            return true;
        } else if (EditorModificationUtil.checkModificationAllowed(editor) && document.getRangeGuard(offset, offset) == null) {
            return !CompletionUtils.isValidDocumentChange(document, offset, event.getOffset());
        } else {
            document.fireReadOnlyModificationAttempt();
            return true;
        }
    }
}
