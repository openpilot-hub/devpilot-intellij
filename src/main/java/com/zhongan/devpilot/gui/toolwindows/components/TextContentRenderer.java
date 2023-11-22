package com.zhongan.devpilot.gui.toolwindows.components;

import com.intellij.ui.JBColor;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.data.DataHolder;

import java.awt.Color;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;

public class TextContentRenderer implements NodeRenderer {

    private static final String DEFAULT_PARAGRAPH_STYLE = "margin-top: 5px; margin-bottom: 5px;";

    private static final String LIST_ITEM_STYLE = "margin-bottom: 5px; margin-left: 3px;";

    @Override
    public @Nullable Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        return Set.of(
            new NodeRenderingHandler<>(Code.class, this::renderCodeSnippets),
            new NodeRenderingHandler<>(Paragraph.class, this::renderTextParagraph)
        );
    }

    private void renderCodeSnippets(Code node, NodeRendererContext context, HtmlWriter html) {
        html.attr("style", "color: " + getRGB(new JBColor(0xA31515, 0xFFC66D)));
        context.delegateRender();
    }

    private String getRGB(Color color) {
        return format("rgb(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void renderTextParagraph(Paragraph node, NodeRendererContext context, HtmlWriter html) {
        Block block = node.getParent();
        String style = block instanceof OrderedListItem || block instanceof BulletListItem
                ? LIST_ITEM_STYLE
                : DEFAULT_PARAGRAPH_STYLE;
        html.attr("style", style);
        context.delegateRender();
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public @NotNull NodeRenderer apply(@NotNull DataHolder dataHolder) {
            return new TextContentRenderer();
        }

    }

}
