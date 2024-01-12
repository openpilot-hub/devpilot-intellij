package com.zhongan.devpilot.completions.inline

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler

class EscapeHandler(private val myOriginalHandler: EditorActionHandler) : EditorActionHandler() {
    public override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        CompletionPreview.clear(editor)
        if (myOriginalHandler.isEnabled(editor, caret, dataContext)) {
            myOriginalHandler.execute(editor, caret, dataContext)
        }
    }

    public override fun isEnabledForCaret(
            editor: Editor,
            caret: Caret,
            dataContext: DataContext
    ): Boolean {
        val preview = CompletionPreview.getInstance(editor)
        return if (preview != null) {
            true
        } else myOriginalHandler.isEnabled(editor, caret, dataContext)
    }

    companion object {
        const val ACTION_ID = "EditorEscape"
    }
}