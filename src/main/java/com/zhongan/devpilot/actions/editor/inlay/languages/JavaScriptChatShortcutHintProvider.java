package com.zhongan.devpilot.actions.editor.inlay.languages;

import com.zhongan.devpilot.actions.editor.inlay.ChatShortcutHintBaseProvider;

import java.util.List;

public class JavaScriptChatShortcutHintProvider extends ChatShortcutHintBaseProvider {

    public JavaScriptChatShortcutHintProvider() {
        super(List.of("JS:FUNCTION_DECLARATION", "JS:FUNCTION_EXPRESSION"));
    }
}
