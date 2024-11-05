package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextArea;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.awt.font.TextAttribute.POSTURE_OBLIQUE;

public class GraphicsUtils {

    private static final @Nullable Method getEditorFontSize2DMethod;

    private static Font editFont;

    static {
        Method method = null;
        if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 221) {
            try {
                method = EditorColorsScheme.class.getMethod("getEditorFontSize2D");
            } catch (NoSuchMethodException var2) {
            }
        }

        getEditorFontSize2DMethod = method;
        JTextArea area = new JTextArea();
        editFont = area.getFont();
    }

    public static Font getFont(Editor editor, boolean deprecated) {
        Font font = editor.getColorsScheme().getFont(EditorFontType.ITALIC);

        // Check if the font family is Monospaced
        if ("JetBrains Mono".equalsIgnoreCase(font.getFamily()) || "Monospaced".equalsIgnoreCase(font.getFamily())) {
            // Create a new font with the font attributes, but change the family to DialogInput
            Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
            attributes.put(TextAttribute.FAMILY, "DialogInput");
            attributes.put(TextAttribute.POSTURE, POSTURE_OBLIQUE);
            font = new Font(attributes);
        }

        // If the font is not deprecated, return it as it is
        if (!deprecated) {
            return font;
        }

        // Create a new font with the font attributes, but add the strikethrough attribute
        Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        return new Font(attributes);
    }

    public static Font getFont(@NotNull Editor editor, @NotNull String text) {

        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN).deriveFont(2);
        Font fallbackFont = UIUtil.getFontWithFallbackIfNeeded(font, text);
        return fallbackFont.deriveFont(fontSize(editor));
    }

    public static float fontSize(@NotNull Editor editor) {

        EditorColorsScheme scheme = editor.getColorsScheme();
        if (getEditorFontSize2DMethod != null) {
            try {
                return (Float) getEditorFontSize2DMethod.invoke(scheme);
            } catch (InvocationTargetException | IllegalAccessException var3) {
            }
        }
        return (float) scheme.getEditorFontSize();
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
        return Math.sqrt((color.getRed() * color.getRed() * 0.241) + (color.getGreen() * color.getGreen() * 0.691) + (color.getBlue() * color.getBlue() * 0.068));
    }

    public static Integer getTabSize(Editor editor) {
        CommonCodeStyleSettings commonCodeStyleSettings = editor.getProject() != null ? PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument()) != null ? new CommonCodeStyleSettings(PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument()).getLanguage()) : null : null;

        return commonCodeStyleSettings != null && commonCodeStyleSettings.getIndentOptions() != null ? commonCodeStyleSettings.getIndentOptions().TAB_SIZE : editor.getSettings().getTabSize(editor.getProject());
    }

}


