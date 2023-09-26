package com.zhongan.codeai;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.zhongan.codeai.actions.editor.popupmenu.PopupMenuEditorActionGroupUtil;

import org.jetbrains.annotations.NotNull;

public class CodeAIStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        PopupMenuEditorActionGroupUtil.refreshActions();
    }
}
