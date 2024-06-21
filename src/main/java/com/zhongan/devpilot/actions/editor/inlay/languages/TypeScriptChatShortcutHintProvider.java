package com.zhongan.devpilot.actions.editor.inlay.languages;

import com.zhongan.devpilot.actions.editor.inlay.ChatShortcutHintBaseProvider;

import java.util.List;

public class TypeScriptChatShortcutHintProvider extends ChatShortcutHintBaseProvider {

    public TypeScriptChatShortcutHintProvider() {
        super(List.of("JS:TYPESCRIPT_FUNCTION", "JS:TYPESCRIPT_FUNCTION_EXPRESSION"));
    }
}
