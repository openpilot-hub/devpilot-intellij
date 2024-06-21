package com.zhongan.devpilot.actions.editor.inlay.languages;

import com.zhongan.devpilot.actions.editor.inlay.ChatShortcutHintBaseProvider;

import java.util.List;

public class JavaChatShortcutHintProvider extends ChatShortcutHintBaseProvider {

    public JavaChatShortcutHintProvider() {
        super(List.of("METHOD"));
    }
}
