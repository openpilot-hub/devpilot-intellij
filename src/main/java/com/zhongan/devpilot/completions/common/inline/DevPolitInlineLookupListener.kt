package com.zhongan.devpilot.completions.common.inline

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
//import com.zhongan.devpilot.completions.common.binary.requests.notifications.shown.SuggestionDroppedReason
import com.zhongan.devpilot.completions.general.DependencyContainer
import java.util.concurrent.atomic.AtomicBoolean

class DevPolitInlineLookupListener : LookupListener {
    private val handler = DependencyContainer.singletonOfInlineCompletionHandler()

    override fun currentItemChanged(event: LookupEvent) {
        var eventItem = event.item;
        System.out.println("-----" + eventItem);
        //TODO 从设置页面获取
        val isCompletionsEnabled = AtomicBoolean(true)
//        if (isCompletionsEnabled.get() || !event.lookup.isFocused || eventItem == null) {
        if (!isCompletionsEnabled.get()) {
            return
        }
        System.out.println("用户输入内容：" + eventItem?.let { event.lookup.itemPattern(it) })
        val editor = event.lookup.editor
        val lastShownSuggestion = CompletionPreview.getCurrentCompletion(editor)
        CompletionPreview.clear(editor)
        InlineCompletionCache.instance.clear(editor)

        val userPrefix = eventItem?.let { event.lookup.itemPattern(it) }
        val completionInFocus = eventItem?.lookupString

        // a weird case when the user presses ctrl+enter but the popup isn't rendered
        // (DocumentChanged event is triggered in this case)
        if (userPrefix == completionInFocus) {
            /*            completionsEventSender.sendSuggestionDropped(
                            editor, lastShownSuggestion, SuggestionDroppedReason.ScrollLookAhead
                        )*/
            return
        }

        if (userPrefix != null && !completionInFocus!!.startsWith(userPrefix)) {
            /*            completionsEventSender.sendSuggestionDropped(
                            editor, lastShownSuggestion, SuggestionDroppedReason.ScrollLookAhead
                        )*/
            return
        }

        userPrefix?.let { completionInFocus?.let { it1 -> LookAheadCompletionAdjustment(it, it1) } }?.let {
            handler.retrieveAndShowCompletion(
                    editor,
                    editor.caretModel.offset,
                    lastShownSuggestion,
                    "",
                    it
            )
        }
    }

    override fun lookupCanceled(event: LookupEvent) {
        // Do nothing, but the validator is furious if we don't implement this.
        // Probably because in older versions this was not implemented.
    }

    override fun itemSelected(event: LookupEvent) {
        // Do nothing, but the validator is furious if we don't implement this.
        // Probably because in older versions this was not implemented.
    }
}
