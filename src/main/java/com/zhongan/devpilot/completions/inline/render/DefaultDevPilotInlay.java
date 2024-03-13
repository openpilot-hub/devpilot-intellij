//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.util.Disposer;
import com.zhongan.devpilot.completions.general.Utils;
import com.zhongan.devpilot.completions.inline.CompletionPreviewInsertionHint;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultDevPilotInlay implements DevPilotInlay {
    private Inlay<?> beforeSuffixInlay;
    private Inlay<?> afterSuffixInlay;
    private Inlay<?> blockInlay;
    private CompletionPreviewInsertionHint insertionHint;

    public DefaultDevPilotInlay(Disposable parent) {
        Disposer.register(parent, this);
    }

    public Integer getOffset() {
        return this.beforeSuffixInlay != null ? this.beforeSuffixInlay.getOffset() : (this.afterSuffixInlay != null ? this.afterSuffixInlay.getOffset() : this.blockInlay != null ? this.blockInlay.getOffset() : null);
    }

    public Rectangle getBounds() {
        Rectangle result = this.beforeSuffixInlay != null ? new Rectangle(this.beforeSuffixInlay.getBounds()) : null;
        if (result != null) {
            Rectangle after = this.afterSuffixInlay != null ? this.afterSuffixInlay.getBounds() : null;
            Rectangle blockBounds = this.blockInlay != null ? this.blockInlay.getBounds() : null;
            if (after != null) {
                result.add(after);
            }

            if (blockBounds != null) {
                result.add(blockBounds);
            }
        }

        return result;
    }

    public Boolean isEmpty() {
        return this.beforeSuffixInlay == null && this.afterSuffixInlay == null && this.blockInlay == null;
    }

    public void dispose() {
        if (this.beforeSuffixInlay != null) {
            Disposer.dispose(this.beforeSuffixInlay);
            this.beforeSuffixInlay = null;
        }

        if (this.afterSuffixInlay != null) {
            Disposer.dispose(this.afterSuffixInlay);
            this.afterSuffixInlay = null;
        }

        if (this.blockInlay != null) {
            Disposer.dispose(this.blockInlay);
            this.blockInlay = null;
        }

    }

    public void render(Editor editor, DevPilotCompletion completion, int offset) {
        List<String> lines = Utils.asLines(completion.getSuffix());
        if (!lines.isEmpty()) {
            String firstLine = (String)lines.get(0);
            int endIndex = firstLine.indexOf(completion.getOldSuffix());
            RenderingInstructions instructions = InlineStringProcessor.determineRendering(lines, completion.getOldSuffix());
            switch (instructions.getFirstLine()) {
                case NoSuffix:
                    this.renderNoSuffix(editor, firstLine, completion, offset);
                    break;
                case SuffixOnly:
                    this.renderAfterSuffix(endIndex, completion, firstLine, editor, offset);
                    break;
                case BeforeAndAfterSuffix:
                    this.renderBeforeSuffix(firstLine, endIndex, editor, completion, offset);
                    this.renderAfterSuffix(endIndex, completion, firstLine, editor, offset);
                case None:
            }

            if (instructions.shouldRenderBlock()) {
                List<String> otherLines = (List)lines.stream().skip(1L).collect(Collectors.toList());
                this.renderBlock(otherLines, editor, completion, offset);
            }

            if (instructions.getFirstLine() != FirstLineRendering.None) {
                this.insertionHint = new CompletionPreviewInsertionHint(editor, this, completion.getSuffix());
            }

        }
    }

    private void renderBlock(List<String> lines, Editor editor, DevPilotCompletion completion, int offset) {
        BlockElementRenderer blockElementRenderer = new BlockElementRenderer(editor, lines, completion.getCompletionMetadata() != null ? completion.getCompletionMetadata().getIsDeprecated() : false);
        Inlay<?> element = editor.getInlayModel().addBlockElement(offset, true, false, 1, blockElementRenderer);
        if (element != null) {
            Disposer.register(this, element);
        }

        this.blockInlay = element;
    }

    private void renderAfterSuffix(int endIndex, DevPilotCompletion completion, String firstLine, Editor editor, int offset) {
        int afterSuffixIndex = endIndex + completion.getOldSuffix().length();
        if (afterSuffixIndex < firstLine.length()) {
            this.afterSuffixInlay = this.renderInline(editor, firstLine.substring(afterSuffixIndex), completion, offset + completion.getOldSuffix().length());
        }

    }

    private void renderBeforeSuffix(String firstLine, int endIndex, Editor editor, DevPilotCompletion completion, int offset) {
        String beforeSuffix = firstLine.substring(0, endIndex);
        this.beforeSuffixInlay = this.renderInline(editor, beforeSuffix, completion, offset);
    }

    private void renderNoSuffix(Editor editor, String firstLine, DevPilotCompletion completion, int offset) {
        this.beforeSuffixInlay = this.renderInline(editor, firstLine, completion, offset);
    }

    private Inlay<?> renderInline(Editor editor, String before, DevPilotCompletion completion, int offset) {
        InlineElementRenderer element = new InlineElementRenderer(editor, before, completion.getCompletionMetadata() != null ? completion.getCompletionMetadata().getIsDeprecated() : false);
        Inlay<InlineElementRenderer> inlay = editor.getInlayModel().addInlineElement(offset, true, element);
        if (inlay != null) {
            Disposer.register(this, inlay);
        }

        return inlay;
    }
}
