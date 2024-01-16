package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

public class EscapeHandler extends EditorActionHandler {
    public static final String ACTION_ID = "EditorEscape";

    private final EditorActionHandler myOriginalHandler;

    public EscapeHandler(EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Override
    public void doExecute(Editor editor, Caret caret, DataContext dataContext) {
        CompletionPreview.clear(editor);
        if (myOriginalHandler.isEnabled(editor, caret, dataContext)) {
            myOriginalHandler.execute(editor, caret, dataContext);
        }
    }

    @Override
    public boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
        CompletionPreview preview = CompletionPreview.getInstance(editor);
        return preview != null || myOriginalHandler.isEnabled(editor, caret, dataContext);
    }
}