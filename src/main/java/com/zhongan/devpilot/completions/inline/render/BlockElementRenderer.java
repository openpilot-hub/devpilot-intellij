package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.TestOnly;

import java.awt.*;
import java.util.List;

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

    @Override
    public int calcWidthInPixels(Inlay inlay) {
        String firstLine = blockText.get(0);
        return editor.getContentComponent()
                .getFontMetrics(GraphicsUtils.getFont(editor, deprecated)).stringWidth(firstLine);
    }

    @Override
    public int calcHeightInPixels(Inlay inlay) {
        return editor.getLineHeight() * blockText.size();
    }

    @Override
    public void paint(Inlay inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        color = color != null ? color : GraphicsUtils.getColor();
        g.setColor(color);
        g.setFont(GraphicsUtils.getFont(editor, deprecated));

        for (int i = 0; i < blockText.size(); i++) {
            String line = blockText.get(i);
            g.drawString(
                    line,
                    0,
                    targetRegion.y + i * editor.getLineHeight() + editor.getAscent()
            );
        }
    }
    @TestOnly
    public String getContent() {
        return String.join("\n", blockText);
    }
}