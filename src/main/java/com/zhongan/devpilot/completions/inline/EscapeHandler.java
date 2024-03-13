//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

public class EscapeHandler extends EditorActionHandler {
    public static final String ACTION_ID = "EditorEscape";
    private final EditorActionHandler myOriginalHandler;

    public EscapeHandler(EditorActionHandler originalHandler) {
        this.myOriginalHandler = originalHandler;
    }

    public void doExecute(Editor editor, Caret caret, DataContext dataContext) {
        CompletionPreview.clear(editor);
        if (this.myOriginalHandler.isEnabled(editor, caret, dataContext)) {
            this.myOriginalHandler.execute(editor, caret, dataContext);
        }

    }

    public boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
        CompletionPreview preview = CompletionPreview.getInstance(editor);
        return preview != null || this.myOriginalHandler.isEnabled(editor, caret, dataContext);
    }
}
