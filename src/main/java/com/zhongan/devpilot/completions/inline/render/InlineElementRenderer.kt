package com.zhongan.devpilot.completions.inline.render

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import org.jetbrains.annotations.TestOnly
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle

class InlineElementRenderer(private val editor: Editor, private val suffix: String, private val deprecated: Boolean) :
        EditorCustomElementRenderer {
    private var color: Color? = null
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        return editor.contentComponent
                .getFontMetrics(GraphicsUtils.getFont(editor, deprecated)).stringWidth(suffix)
    }

    @TestOnly
    fun getContent(): String {
        return suffix
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
        g.drawString(suffix, targetRegion.x, targetRegion.y + editor.ascent)
    }
}
