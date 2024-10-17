package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.zhongan.devpilot.completions.inline.CompletionPreview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.jetbrains.annotations.TestOnly;

public class InlineElementRenderer implements EditorCustomElementRenderer {
    private final Editor editor;

    private final String suffix;

    private boolean deprecated;

    private Color color;

    public InlineElementRenderer(Editor editor, String suffix, boolean deprecated, boolean needHint) {
        this.editor = editor;
        this.suffix = needHint ? suffix + "       " + CompletionPreview.byLineAcceptHintText() : suffix;
        this.deprecated = deprecated;
    }

    public InlineElementRenderer(Editor editor, String suffix, boolean deprecated) {
        this(editor, suffix, deprecated, false);
    }

    @Override
    public int calcWidthInPixels(Inlay inlay) {
        return editor.getContentComponent()
                .getFontMetrics(GraphicsUtils.getFont(editor, suffix)).stringWidth(suffix);
    }

    @TestOnly
    public String getContent() {
        return suffix;
    }

    @Override
    public void paint(Inlay inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        color = color != null ? color : GraphicsUtils.getColor();
        g.setColor(color);
        g.setFont(GraphicsUtils.getFont(editor, suffix));
        g.drawString(suffix, targetRegion.x, targetRegion.y + editor.getAscent());
    }
}
