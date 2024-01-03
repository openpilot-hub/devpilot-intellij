package com.zhongan.devpilot.common.inline

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.BaseCodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ShowNextTabnineInlineCompletionAction :
    BaseCodeInsightAction(false),
    DumbAware,
    InlineCompletionAction {
    companion object {
        const val ACTION_ID = "ShowNextTabnineInlineCompletionAction"
    }

    override fun getHandler(): CodeInsightActionHandler {
        return CodeInsightActionHandler { _: Project?, editor: Editor, _: PsiFile? ->
            CompletionPreview.getInstance(editor)?.togglePreview(CompletionOrder.NEXT)
        }
    }

    override fun isValidForLookup(): Boolean = true
}
