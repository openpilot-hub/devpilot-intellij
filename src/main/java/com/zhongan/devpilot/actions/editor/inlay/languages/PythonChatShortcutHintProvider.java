package com.zhongan.devpilot.actions.editor.inlay.languages;

import com.zhongan.devpilot.actions.editor.inlay.ChatShortcutHintBaseProvider;

import java.util.List;

public class PythonChatShortcutHintProvider extends ChatShortcutHintBaseProvider {

    public PythonChatShortcutHintProvider() {
        super(List.of("Py:FUNCTION_DECLARATION"));
    }
}
