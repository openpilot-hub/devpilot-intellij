package com.zhongan.devpilot.common.inline.render

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Disposer
import com.zhongan.devpilot.common.general.Utils
import com.zhongan.devpilot.common.inline.CompletionPreviewInsertionHint
import com.zhongan.devpilot.common.prediction.DevPilotCompletion
import java.awt.Rectangle
import java.util.stream.Collectors

class DefaultTabnineInlay(parent: Disposable) : TabnineInlay {
    private var beforeSuffixInlay: Inlay<*>? = null
    private var afterSuffixInlay: Inlay<*>? = null
    private var blockInlay: Inlay<*>? = null
    private var insertionHint: CompletionPreviewInsertionHint? = null

    init {
        Disposer.register(parent, this)
    }

    override val offset: Int?
        get() = beforeSuffixInlay?.offset ?: afterSuffixInlay?.offset ?: blockInlay?.offset

    override fun getBounds(): Rectangle? {
        val result = beforeSuffixInlay?.bounds?.let { Rectangle(it) }

        result?.bounds?.let {
            afterSuffixInlay?.bounds?.let { after -> result.add(after) }
            blockInlay?.bounds?.let { blockBounds -> result.add(blockBounds) }
        }

        return result
    }

    override val isEmpty: Boolean
        get() = beforeSuffixInlay == null && afterSuffixInlay == null && blockInlay == null

    override fun dispose() {
        beforeSuffixInlay?.let {
            Disposer.dispose(it)
            beforeSuffixInlay = null
        }
        afterSuffixInlay?.let {
            Disposer.dispose(it)
            afterSuffixInlay = null
        }
        blockInlay?.let {
            Disposer.dispose(it)
            blockInlay = null
        }
    }

    override fun render(editor: Editor, completion: DevPilotCompletion, offset: Int) {
        val lines = Utils.asLines(completion.suffix)
        if (lines.isEmpty()) return
        val firstLine = lines[0]
        val endIndex = firstLine.indexOf(completion.oldSuffix)

        val instructions = determineRendering(lines, completion.oldSuffix)

        when (instructions.firstLine) {
            FirstLineRendering.NoSuffix -> {
                renderNoSuffix(editor, firstLine, completion, offset)
            }
            FirstLineRendering.SuffixOnly -> {
                renderAfterSuffix(endIndex, completion, firstLine, editor, offset)
            }
            FirstLineRendering.BeforeAndAfterSuffix -> {
                renderBeforeSuffix(firstLine, endIndex, editor, completion, offset)
                renderAfterSuffix(endIndex, completion, firstLine, editor, offset)
            }
            FirstLineRendering.None -> {}
        }

        if (instructions.shouldRenderBlock) {
            val otherLines = lines.stream().skip(1).collect(Collectors.toList())
            renderBlock(otherLines, editor, completion, offset)
        }

        if (instructions.firstLine != FirstLineRendering.None) {
            insertionHint = CompletionPreviewInsertionHint(editor, this, completion.suffix)
        }
    }

    private fun renderBlock(
            lines: List<String>,
            editor: Editor,
            completion: DevPilotCompletion,
            offset: Int
    ) {
        val blockElementRenderer = BlockElementRenderer(editor, lines, completion.completionMetadata?.deprecated ?: false)
        val element = editor
            .inlayModel
            .addBlockElement(
                offset,
                true,
                false,
                1,
                blockElementRenderer
            )

        element?.let { Disposer.register(this, it) }

        blockInlay = element
    }

    private fun renderAfterSuffix(
            endIndex: Int,
            completion: DevPilotCompletion,
            firstLine: String,
            editor: Editor,
            offset: Int
    ) {
        val afterSuffixIndex = endIndex + completion.oldSuffix.length
        if (afterSuffixIndex < firstLine.length) {
            afterSuffixInlay = renderInline(
                editor,
                firstLine.substring(afterSuffixIndex),
                completion,
                offset + completion.oldSuffix.length
            )
        }
    }

    private fun renderBeforeSuffix(
            firstLine: String,
            endIndex: Int,
            editor: Editor,
            completion: DevPilotCompletion,
            offset: Int
    ) {
        val beforeSuffix = firstLine.substring(0, endIndex)
        beforeSuffixInlay = renderInline(editor, beforeSuffix, completion, offset)
    }

    private fun renderNoSuffix(
            editor: Editor,
            firstLine: String,
            completion: DevPilotCompletion,
            offset: Int
    ) {
        beforeSuffixInlay = renderInline(editor, firstLine, completion, offset)
    }

    private fun renderInline(
            editor: Editor,
            before: String,
            completion: DevPilotCompletion,
            offset: Int
    ): Inlay<InlineElementRenderer>? {
        val element = editor
            .inlayModel
            .addInlineElement(offset, true, InlineElementRenderer(editor, before, completion.completionMetadata?.deprecated ?: false))

        element?.let { Disposer.register(this, it) }

        return element
    }
}
