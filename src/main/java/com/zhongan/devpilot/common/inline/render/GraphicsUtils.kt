package com.zhongan.devpilot.common.inline.render

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.ui.JBColor
import com.zhongan.devpilot.common.capabilities.CapabilitiesService
import com.zhongan.devpilot.common.capabilities.Capability
import com.zhongan.devpilot.common.userSettings.AppSettingsState
import java.awt.Color
import java.awt.Font
import java.awt.font.TextAttribute
import kotlin.math.abs
import kotlin.math.sqrt

object GraphicsUtils {
    fun getFont(editor: Editor, deprecated: Boolean): Font {
        val font = editor.colorsScheme.getFont(EditorFontType.ITALIC)
        if (!deprecated) {
            return font
        }
        val attributes: MutableMap<TextAttribute, Any?> = HashMap(font.attributes)
        attributes[TextAttribute.STRIKETHROUGH] = TextAttribute.STRIKETHROUGH_ON
        return Font(attributes)
    }

    val color: Color
        get() {
            return Color(AppSettingsState.instance.inlineHintColor)
        }

    val niceContrastColor: Color
        get() {
            val averageBrightness = (getBrightness(JBColor.background()) + getBrightness(JBColor.foreground())) / 2.0
            var currentResult = Color.lightGray
            var bestResult = currentResult
            var distance = Double.MAX_VALUE
            var currentBrightness = getBrightness(currentResult)
            val minBrightness = getBrightness(Color.darkGray)

            while (currentBrightness > minBrightness) {
                if (abs(currentBrightness - averageBrightness) < distance) {
                    distance = abs(currentBrightness - averageBrightness)
                    bestResult = currentResult
                }
                currentResult = currentResult.darker()
                currentBrightness = getBrightness(currentResult)
            }
            return bestResult
        }

    private fun getBrightness(color: Color): Double {
        return sqrt(
            (color.red * color.red * 0.241) +
                (color.green * color.green * 0.691) +
                (color.blue * color.blue * 0.068)
        )
    }
}

fun getTabSize(editor: Editor): Int? {
    if (!ApplicationManager.getApplication().isReadAccessAllowed) {
        Logger.getInstance("GraphicsUtils").warn("Read access is not allowed here - returning null")
        failIfAlpha()
        return null
    }
    val commonCodeStyleSettings = editor.project
        ?.let { PsiDocumentManager.getInstance(it).getPsiFile(editor.document) }
        ?.let { CommonCodeStyleSettings(it.language) }

    return commonCodeStyleSettings?.indentOptions?.TAB_SIZE ?: editor.settings.getTabSize(editor.project)
}

private fun failIfAlpha() {
    val isAlpha = CapabilitiesService.getInstance().isCapabilityEnabled(
        Capability.ALPHA
    )
    val isTest = ApplicationManager.getApplication().isUnitTestMode
    if (isAlpha && !isTest) {
        Logger.getInstance("GraphicsUtils")
            .error("!!!Alpha user please notice!!! You called `getTabSize` from a thread without read access. Because you're alpha, a `RuntimeException` will be thrown - This is being done in order to cause chaos for alpha devs, so that they'll fix it.")
        throw RuntimeException("You called `getTabSize` from a thread without read access!")
    }
}
