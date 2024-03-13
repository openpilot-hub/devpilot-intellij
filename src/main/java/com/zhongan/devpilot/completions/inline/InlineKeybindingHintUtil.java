//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import com.zhongan.devpilot.DevPilotIcons;
import java.awt.Point;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

public class InlineKeybindingHintUtil {
    public InlineKeybindingHintUtil() {
    }

    public static void createAndShowHint(@NotNull Editor editor, @NotNull Point pos) {


        try {
            HintManagerImpl.getInstanceImpl().showEditorHint(new LightweightHint(createInlineHintComponent()), editor, pos, 130, 0, false);
        } catch (Throwable var3) {
            Logger.getInstance(InlineKeybindingHintUtil.class).warn("Failed to show inline key bindings hints", var3);
        }

    }

    private static JComponent createInlineHintComponent() {
        SimpleColoredComponent component = HintUtil.createInformationComponent();
        component.setIconOnTheRight(true);
        component.setIcon(DevPilotIcons.SYSTEM_ICON);
        SimpleColoredText coloredText = new SimpleColoredText(hintText(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        coloredText.appendToComponent(component);
        return new InlineKeybindingHintComponent(component);
    }

    private static String hintText() {
        String acceptShortcut = getShortcutText("AcceptDevPilotInlineCompletionAction");
        String cancelShortcut = KeymapUtil.getKeyText(27);
        return String.format("Accept (%s) Cancel (%s)", acceptShortcut, cancelShortcut);
    }

    private static String getShortcutText(String actionId) {
        return StringUtil.defaultIfEmpty(KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(actionId)), "Missing shortcut key");
    }
}
