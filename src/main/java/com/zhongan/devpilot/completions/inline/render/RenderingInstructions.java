package com.zhongan.devpilot.completions.inline.render;

public class RenderingInstructions {
    private FirstLineRendering firstLine;

    private boolean shouldRenderBlock;

    public RenderingInstructions(FirstLineRendering firstLine, boolean shouldRenderBlock) {
        this.firstLine = firstLine;
        this.shouldRenderBlock = shouldRenderBlock;
    }

    public FirstLineRendering getFirstLine() {
        return firstLine;
    }

    public boolean shouldRenderBlock() {
        return shouldRenderBlock;
    }
}
