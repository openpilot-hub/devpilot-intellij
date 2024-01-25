package com.zhongan.devpilot.completions.inline.render;

import java.util.List;

public class InlineStringProcessor {

    public static RenderingInstructions determineRendering(List<String> textLines, String oldSuffix) {
        if (textLines.isEmpty()) {
            return new RenderingInstructions(FirstLineRendering.None, false);
        }

        boolean shouldRenderBlock = textLines.size() > 1;

        if (!textLines.get(0).trim().isEmpty()) {
            if (!oldSuffix.trim().isEmpty()) {
                int endIndex = textLines.get(0).indexOf(oldSuffix);

                if (endIndex == 0) {
                    return new RenderingInstructions(FirstLineRendering.SuffixOnly, shouldRenderBlock);
                } else if (endIndex > 0) {
                    return new RenderingInstructions(FirstLineRendering.BeforeAndAfterSuffix, shouldRenderBlock);
                }
            }

            return new RenderingInstructions(FirstLineRendering.NoSuffix, shouldRenderBlock);
        }

        return new RenderingInstructions(FirstLineRendering.None, shouldRenderBlock);
    }

}
