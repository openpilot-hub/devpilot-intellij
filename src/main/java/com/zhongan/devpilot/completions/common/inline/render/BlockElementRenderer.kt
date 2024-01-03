package com.zhongan.devpilot.completions.common.inline.render

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import org.jetbrains.annotations.TestOnly
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle

class BlockElementRenderer(
    private val editor: Editor,
    private val blockText: List<String>,
    private val deprecated: Boolean
) : EditorCustomElementRenderer {
    private var color: Color? = null

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val firstLine = blockText[0]
        return editor.contentComponent
            .getFontMetrics(GraphicsUtils.getFont(editor, deprecated)).stringWidth(firstLine)
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return editor.lineHeight * blockText.size
    }

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes
    ) {
        color = color ?: GraphicsUtils.color
        g.color = color
        g.font = GraphicsUtils.getFont(editor, deprecated)

        blockText.withIndex().forEach { (i, line) ->
            g.drawString(
                line,
                0,
                targetRegion.y + i * editor.lineHeight + editor.ascent
            )
        }
    }

    @TestOnly
    fun getContent(): String {
        return blockText.joinToString("\n")
    }
}
