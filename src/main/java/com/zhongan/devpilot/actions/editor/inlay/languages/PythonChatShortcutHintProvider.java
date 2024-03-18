package com.zhongan.devpilot.actions.editor.inlay.languages;

import com.zhongan.devpilot.actions.editor.inlay.ChatShortcutHintBaseProvider;

import java.util.Arrays;

public class PythonChatShortcutHintProvider extends ChatShortcutHintBaseProvider {

    public PythonChatShortcutHintProvider() {
        super(Arrays.asList("Py:CLASS_DECLARATION", "Py:FUNCTION_DECLARATION"));
    }
}
