package com.zhongan.devpilot.actions.editor.inlay.languages;

import com.zhongan.devpilot.actions.editor.inlay.ChatShortcutHintBaseProvider;

import java.util.List;

public class GoChatShortcutHintProvider extends ChatShortcutHintBaseProvider {

    public GoChatShortcutHintProvider() {
        super(List.of("FUNCTION_DECLARATION", "METHOD_DECLARATION"));
    }
}
