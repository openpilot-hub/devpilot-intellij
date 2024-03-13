//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.general;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;

public class EditorUtils {
    public EditorUtils() {
    }

    public static boolean isMainEditor(Editor editor) {
        return editor.getEditorKind() == EditorKind.MAIN_EDITOR || ApplicationManager.getApplication().isUnitTestMode();
    }
}
