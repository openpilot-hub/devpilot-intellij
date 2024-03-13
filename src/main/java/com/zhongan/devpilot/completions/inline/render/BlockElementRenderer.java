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
import java.util.List;
import org.jetbrains.annotations.TestOnly;

public class BlockElementRenderer implements EditorCustomElementRenderer {
    private Editor editor;
    private List<String> blockText;
    private boolean deprecated;
    private Color color;

    public BlockElementRenderer(Editor editor, List<String> blockText, boolean deprecated) {
        this.editor = editor;
        this.blockText = blockText;
        this.deprecated = deprecated;
    }

    public int calcWidthInPixels(Inlay inlay) {
        String firstLine = (String)this.blockText.get(0);
        return this.editor.getContentComponent().getFontMetrics(GraphicsUtils.getFont(this.editor, this.deprecated)).stringWidth(firstLine);
    }

    public int calcHeightInPixels(Inlay inlay) {
        return this.editor.getLineHeight() * this.blockText.size();
    }

    public void paint(Inlay inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        this.color = this.color != null ? this.color : GraphicsUtils.getColor();
        g.setColor(this.color);
        g.setFont(GraphicsUtils.getFont(this.editor, this.deprecated));

        for(int i = 0; i < this.blockText.size(); ++i) {
            String line = (String)this.blockText.get(i);
            g.drawString(line, 0, targetRegion.y + i * this.editor.getLineHeight() + this.editor.getAscent());
        }

    }

    @TestOnly
    public String getContent() {
        return String.join("\n", this.blockText);
    }
}
