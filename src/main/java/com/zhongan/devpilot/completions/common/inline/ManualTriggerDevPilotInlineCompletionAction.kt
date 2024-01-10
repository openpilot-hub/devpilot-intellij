package com.zhongan.devpilot.completions.common.inline

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.BaseCodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
//import com.zhongan.devpilot.completions.common.capabilities.RenderingMode
import com.zhongan.devpilot.completions.common.general.DependencyContainer

class ManualTriggerDevPilotInlineCompletionAction :
    BaseCodeInsightAction(false),
    DumbAware,
        InlineCompletionAction {
    companion object {
        const val ACTION_ID = "ManualTriggerDevPilotInlineCompletionAction"
    }

    private val handler = DependencyContainer.singletonOfInlineCompletionHandler()
//    private val completionsEventSender = DependencyContainer.instanceOfCompletionsEventSender()

    override fun getHandler(): CodeInsightActionHandler {
        return CodeInsightActionHandler { _: Project?, editor: Editor, _: PsiFile? ->
//            completionsEventSender.sendManualSuggestionTrigger(RenderingMode.INLINE)
            val lastShownCompletion = CompletionPreview.getCurrentCompletion(editor)

            handler.retrieveAndShowCompletion(
                editor, editor.caretModel.offset, lastShownCompletion, "",
                DefaultCompletionAdjustment()
            )
        }
    }

    override fun isValidForLookup(): Boolean = true
}
