package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.zhongan.devpilot.completions.inline.CompletionPreview;

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

    private final boolean needHintInBlock;

    public BlockElementRenderer(Editor editor, List<String> blockText, boolean deprecated, boolean needHintInBlock) {
        this.editor = editor;
        this.blockText = blockText;
        this.deprecated = deprecated;
        this.needHintInBlock = needHintInBlock;
    }

    @Override
    public int calcWidthInPixels(Inlay inlay) {
        String firstLine = blockText.get(0);
        boolean hint = blockText.size() > 1;
        if (needHintInBlock && hint) {
            firstLine = firstLine + "       " + CompletionPreview.byLineAcceptHintText();
        }
        return editor.getContentComponent()
                .getFontMetrics(GraphicsUtils.getFont(editor, firstLine)).stringWidth(firstLine);
    }

    @Override
    public int calcHeightInPixels(Inlay inlay) {
        return editor.getLineHeight() * blockText.size();
    }

    @Override
    public void paint(Inlay inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        boolean hint = blockText.size() > 1;
        color = color != null ? color : GraphicsUtils.getColor();
        g.setColor(color);
        for (int i = 0; i < blockText.size(); i++) {
            String line = blockText.get(i);
            if (needHintInBlock && i == 0 && hint) {
                line = line + "       " + CompletionPreview.byLineAcceptHintText();
                hint = false;
            }
            g.setFont(GraphicsUtils.getFont(editor, line));
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