//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline.render;

import java.util.List;

public class InlineStringProcessor {
    public InlineStringProcessor() {
    }

    public static RenderingInstructions determineRendering(List<String> textLines, String oldSuffix) {
        if (textLines.isEmpty()) {
            return new RenderingInstructions(FirstLineRendering.None, false);
        } else {
            boolean shouldRenderBlock = textLines.size() > 1;
            if (!((String)textLines.get(0)).trim().isEmpty()) {
                if (!oldSuffix.trim().isEmpty()) {
                    int endIndex = ((String)textLines.get(0)).indexOf(oldSuffix);
                    if (endIndex == 0) {
                        return new RenderingInstructions(FirstLineRendering.SuffixOnly, shouldRenderBlock);
                    }

                    if (endIndex > 0) {
                        return new RenderingInstructions(FirstLineRendering.BeforeAndAfterSuffix, shouldRenderBlock);
                    }
                }

                return new RenderingInstructions(FirstLineRendering.NoSuffix, shouldRenderBlock);
            } else {
                return new RenderingInstructions(FirstLineRendering.None, shouldRenderBlock);
            }
        }
    }
}
