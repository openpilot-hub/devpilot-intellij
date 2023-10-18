package com.zhongan.codeai.util;

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

public class ContentNodeRenderer implements NodeRenderer {
    @Override
    public @Nullable Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        return Set.of(
            new NodeRenderingHandler<>(Code.class, this::renderCodeSnippets),
            new NodeRenderingHandler<>(Paragraph.class, this::renderTextParagraph)
        );
    }

    private void renderTextParagraph(Paragraph node, NodeRendererContext context, HtmlWriter html) {
        Block block = node.getParent();
        if (block instanceof OrderedListItem || block instanceof BulletListItem) {
            html.attr("style", "margin: 0; padding:0;");
        } else {
            html.attr("style", "margin-top: 4px; margin-bottom: 4px;");
        }
        context.delegateRender();
    }

    private void renderCodeSnippets(Code node, NodeRendererContext context, HtmlWriter html) {
        html.attr("style", "color: " + getRGB(new JBColor(0xFFC66D, 0xFFC66D)));
        context.delegateRender();
    }

    private String getRGB(Color color) {
        return format("rgb(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public @NotNull NodeRenderer apply(@NotNull DataHolder dataHolder) {
            return new ContentNodeRenderer();
        }

    }

}
