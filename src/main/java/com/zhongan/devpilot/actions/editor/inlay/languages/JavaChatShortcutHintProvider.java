package com.zhongan.devpilot.actions.editor.inlay.languages;

import com.zhongan.devpilot.actions.editor.inlay.ChatShortcutHintBaseProvider;

import java.util.Arrays;

public class JavaChatShortcutHintProvider extends ChatShortcutHintBaseProvider {

    public JavaChatShortcutHintProvider() {
        super(Arrays.asList("CLASS", "METHOD"));
    }
}
