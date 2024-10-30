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

    @Override
    public Integer getOffset() {
        return beforeSuffixInlay != null ? beforeSuffixInlay.getOffset() :
                afterSuffixInlay != null ? afterSuffixInlay.getOffset() :
                        blockInlay != null ? blockInlay.getOffset() : null;
    }

    @Override
    public Rectangle getBounds() {
        Rectangle result = beforeSuffixInlay != null ? new Rectangle(beforeSuffixInlay.getBounds()) : null;

        if (result != null) {
            Rectangle after = afterSuffixInlay != null ? afterSuffixInlay.getBounds() : null;
            Rectangle blockBounds = blockInlay != null ? blockInlay.getBounds() : null;

            if (after != null) {
                result.add(after);
            }
            if (blockBounds != null) {
                result.add(blockBounds);
            }
        }

        return result;
    }

    @Override
    public Boolean isEmpty() {
        return beforeSuffixInlay == null && afterSuffixInlay == null && blockInlay == null;
    }

    @Override
    public void dispose() {
        if (beforeSuffixInlay != null) {
            Disposer.dispose(beforeSuffixInlay);
            beforeSuffixInlay = null;
        }
        if (afterSuffixInlay != null) {
            Disposer.dispose(afterSuffixInlay);
            afterSuffixInlay = null;
        }
        if (blockInlay != null) {
            Disposer.dispose(blockInlay);
            blockInlay = null;
        }
    }

    @Override
    public void render(Editor editor, DevPilotCompletion completion, int offset) {
        List<String> lines = Utils.asLines(completion.getSuffix());
        if (lines.isEmpty()) return;
        String firstLine = lines.get(0);
        int endIndex = firstLine.indexOf(completion.getOldSuffix());

        RenderingInstructions instructions = InlineStringProcessor.determineRendering(lines, completion.getOldSuffix());
        boolean needHintInBlock = true;
        switch (instructions.getFirstLine()) {
            case NoSuffix:
                needHintInBlock = false;
                renderNoSuffix(editor, firstLine, completion, offset, lines.size() > 1);
                break;

            case SuffixOnly:
                needHintInBlock = false;
                renderAfterSuffix(endIndex, completion, firstLine, editor, offset);
                break;

            case BeforeAndAfterSuffix:
                needHintInBlock = false;
                renderBeforeSuffix(firstLine, endIndex, editor, completion, offset);
                renderAfterSuffix(endIndex, completion, firstLine, editor, offset);
                break;

            case None:
                break;
        }

        if (instructions.shouldRenderBlock()) {
            List<String> otherLines = lines.stream().skip(1).collect(Collectors.toList());
            renderBlock(otherLines, editor, completion, offset, needHintInBlock);
        }

        if (instructions.getFirstLine() != FirstLineRendering.None) {
            insertionHint = new CompletionPreviewInsertionHint(editor, this, completion.getSuffix());
        }
    }

    private void renderBlock(List<String> lines, Editor editor, DevPilotCompletion completion, int offset, boolean needHintInBlock) {
        BlockElementRenderer blockElementRenderer = new BlockElementRenderer(editor, lines, completion.getCompletionMetadata() != null ?
                completion.getCompletionMetadata().getIsDeprecated() : false, needHintInBlock);
        Inlay<?> element = editor.getInlayModel().addBlockElement(offset, true, false, 1, blockElementRenderer);
        if (element != null) {
            Disposer.register(this, element);
        }
        blockInlay = element;
    }

    private void renderAfterSuffix(int endIndex, DevPilotCompletion completion, String firstLine, Editor editor, int offset) {
        int afterSuffixIndex = endIndex + completion.getOldSuffix().length();
        if (afterSuffixIndex < firstLine.length()) {
            afterSuffixInlay = renderInline(editor, firstLine.substring(afterSuffixIndex), completion, offset + completion.getOldSuffix().length());
        }
    }

    private void renderBeforeSuffix(String firstLine, int endIndex, Editor editor, DevPilotCompletion completion, int offset) {
        String beforeSuffix = firstLine.substring(0, endIndex);
        beforeSuffixInlay = renderInline(editor, beforeSuffix, completion, offset);
    }

    private void renderNoSuffix(Editor editor, String firstLine, DevPilotCompletion completion, int offset, boolean needHint) {
        beforeSuffixInlay = renderInline(editor, firstLine, completion, offset, needHint);
    }

    private Inlay<?> renderInline(Editor editor, String before, DevPilotCompletion completion, int offset, boolean needHint) {
        InlineElementRenderer element = new InlineElementRenderer(editor, before, completion.getCompletionMetadata() != null ?
                completion.getCompletionMetadata().getIsDeprecated() : false, needHint);
        Inlay<InlineElementRenderer> inlay = editor.getInlayModel().addInlineElement(offset, true, element);
        if (inlay != null) {
            Disposer.register(this, inlay);
        }
        return inlay;
    }

    private Inlay<?> renderInline(Editor editor, String before, DevPilotCompletion completion, int offset) {
        return this.renderInline(editor, before, completion, offset, false);
    }

}
