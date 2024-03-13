//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import org.jetbrains.annotations.TestOnly;

public class InlineElementRenderer implements EditorCustomElementRenderer {
    private final Editor editor;
    private final String suffix;
    private boolean deprecated;
    private Color color;

    public InlineElementRenderer(Editor editor, String suffix, boolean deprecated) {
        this.editor = editor;
        this.suffix = suffix;
        this.deprecated = deprecated;
    }

    public int calcWidthInPixels(Inlay inlay) {
        return this.editor.getContentComponent().getFontMetrics(GraphicsUtils.getFont(this.editor, this.deprecated)).stringWidth(this.suffix);
    }

    @TestOnly
    public String getContent() {
        return this.suffix;
    }

    public void paint(Inlay inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        this.color = this.color != null ? this.color : GraphicsUtils.getColor();
        g.setColor(this.color);
        g.setFont(GraphicsUtils.getFont(this.editor, this.deprecated));
        g.drawString(this.suffix, targetRegion.x, targetRegion.y + this.editor.getAscent());
    }
}
