package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.zhongan.devpilot.completions.inline.AcceptDevPilotInlineCompletionByLineAction;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

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

    @Override
    public int calcWidthInPixels(Inlay inlay) {
        String firstLine = blockText.get(0);
        boolean hint = blockText.size() > 1;
        if (hint) {
            firstLine = firstLine + "       " + hintText();
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
            if (i == 0 && hint) {
                line = line + "       " + hintText();
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

    private String hintText() {
        String acceptShortcut = getShortcutText();
        return String.format("%s %s", acceptShortcut, DevPilotMessageBundle.get("completion.apply.partial.tooltips"));
    }

    private String getShortcutText() {
        return StringUtil.defaultIfEmpty(
                KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(AcceptDevPilotInlineCompletionByLineAction.ACTION_ID)),
                "Missing shortcut key");
    }

    @TestOnly
    public String getContent() {
        return String.join("\n", blockText);
    }
}