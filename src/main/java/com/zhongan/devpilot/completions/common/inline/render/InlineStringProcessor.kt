package com.zhongan.devpilot.completions.common.inline.render

enum class FirstLineRendering {
    None,
    NoSuffix,
    SuffixOnly,
    BeforeAndAfterSuffix,
}

data class RenderingInstructions(val firstLine: FirstLineRendering, val shouldRenderBlock: Boolean)

fun determineRendering(textLines: List<String>, oldSuffix: String): RenderingInstructions {
    if (textLines.isEmpty()) return RenderingInstructions(FirstLineRendering.None, false)

    val shouldRenderBlock = textLines.size > 1

    if (textLines[0].trim().isNotEmpty()) {
        if (oldSuffix.trim().isNotEmpty()) {
            val endIndex = textLines[0].indexOf(oldSuffix)

            if (endIndex == 0) return RenderingInstructions(FirstLineRendering.SuffixOnly, shouldRenderBlock)
            else if (endIndex > 0) return RenderingInstructions(
                FirstLineRendering.BeforeAndAfterSuffix,
                shouldRenderBlock
            )
        }

        return RenderingInstructions(FirstLineRendering.NoSuffix, shouldRenderBlock)
    }

    return RenderingInstructions(FirstLineRendering.None, shouldRenderBlock)
}
