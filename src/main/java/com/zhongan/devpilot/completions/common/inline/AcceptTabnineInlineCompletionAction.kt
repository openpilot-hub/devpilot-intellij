package com.zhongan.devpilot.completions.common.inline

import com.intellij.codeInsight.hint.HintManagerImpl.ActionToIgnore
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler

object AcceptDevPilotInlineCompletionAction :
    EditorAction(AcceptInlineCompletionHandler()),
    ActionToIgnore,
        InlineCompletionAction {
    const val ACTION_ID = "AcceptDevPilotInlineCompletionAction"

    class AcceptInlineCompletionHandler : EditorWriteActionHandler() {
        override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext) {
            CompletionPreview.getInstance(editor)?.applyPreview(caret ?: editor.caretModel.currentCaret)
        }

        override fun isEnabledForCaret(
            editor: Editor,
            caret: Caret,
            dataContext: DataContext
        ): Boolean {
            return CompletionPreview.getInstance(editor) != null
        }
    }
}
