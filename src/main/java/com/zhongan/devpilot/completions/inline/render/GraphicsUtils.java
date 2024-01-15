package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.ui.JBColor;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class GraphicsUtils {
    public static Font getFont(Editor editor, boolean deprecated) {
        Font font = editor.getColorsScheme().getFont(EditorFontType.ITALIC);
        if (!deprecated) {
            return font;
        }
        Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        return new Font(attributes);
    }

    public static Color getColor() {
        return new Color(niceContrastColor().getRGB());
    }

    public static Color niceContrastColor() {
        double averageBrightness = (getBrightness(JBColor.background()) + getBrightness(JBColor.foreground())) / 2.0;
        Color currentResult = Color.lightGray;
        Color bestResult = currentResult;
        double distance = Double.MAX_VALUE;
        double currentBrightness = getBrightness(currentResult);
        double minBrightness = getBrightness(Color.darkGray);

        while (currentBrightness > minBrightness) {
            if (Math.abs(currentBrightness - averageBrightness) < distance) {
                distance = Math.abs(currentBrightness - averageBrightness);
                bestResult = currentResult;
            }
            currentResult = currentResult.darker();
            currentBrightness = getBrightness(currentResult);
        }
        return bestResult;
    }

    private static double getBrightness(Color color) {
        return Math.sqrt(
                (color.getRed() * color.getRed() * 0.241) +
                        (color.getGreen() * color.getGreen() * 0.691) +
                        (color.getBlue() * color.getBlue() * 0.068)
        );
    }

    public static Integer getTabSize(Editor editor) {
        if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
            Logger.getInstance("GraphicsUtils").warn("Read access is not allowed here - returning null");
            failIfAlpha();
            return null;
        }
        CommonCodeStyleSettings commonCodeStyleSettings = editor.getProject() != null
                ? PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument()) != null
                ? new CommonCodeStyleSettings(PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument()).getLanguage())
                : null
                : null;

        return commonCodeStyleSettings != null && commonCodeStyleSettings.getIndentOptions() != null ?
                commonCodeStyleSettings.getIndentOptions().TAB_SIZE : editor.getSettings().getTabSize(editor.getProject());
    }

    private static void failIfAlpha() {
        boolean isAlpha = true;
        boolean isTest = ApplicationManager.getApplication().isUnitTestMode();
        if (isAlpha && !isTest) {
            Logger.getInstance("GraphicsUtils")
                    .error("!!!Alpha user please notice!!! You called `getTabSize` from a thread without read access. Because you're alpha, a `RuntimeException` will be thrown - This is being done in order to cause chaos for alpha devs, so that they'll fix it.");
            throw new RuntimeException("You called `getTabSize` from a thread without read access!");
        }
    }

}


