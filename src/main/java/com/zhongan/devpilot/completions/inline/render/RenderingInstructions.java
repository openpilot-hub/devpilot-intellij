//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline.render;

public class RenderingInstructions {
    private FirstLineRendering firstLine;
    private boolean shouldRenderBlock;

    public RenderingInstructions(FirstLineRendering firstLine, boolean shouldRenderBlock) {
        this.firstLine = firstLine;
        this.shouldRenderBlock = shouldRenderBlock;
    }

    public FirstLineRendering getFirstLine() {
        return this.firstLine;
    }

    public boolean shouldRenderBlock() {
        return this.shouldRenderBlock;
    }
}
