package com.zhongan.devpilot.common.inline.listeners

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.util.Disposer
import com.intellij.util.ObjectUtils
import com.zhongan.devpilot.common.binary.requests.notifications.shown.SuggestionDroppedReason
import com.zhongan.devpilot.common.general.DependencyContainer
import com.zhongan.devpilot.common.inline.CompletionPreview

class InlineFocusListener(private val completionPreview: CompletionPreview) : FocusChangeListener {
    private val completionsEventSender = DependencyContainer.instanceOfCompletionsEventSender()
    init {
        ObjectUtils.consumeIfCast(
            completionPreview.editor, EditorEx::class.java
        ) { e: EditorEx -> e.addFocusListener(this, completionPreview) }
    }

    override fun focusGained(editor: Editor) {}
    override fun focusLost(editor: Editor) {
        completionsEventSender.sendSuggestionDropped(
            editor, completionPreview.currentCompletion, SuggestionDroppedReason.FocusChanged
        )

        Disposer.dispose(completionPreview)
    }
}
