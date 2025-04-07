package com.zhongan.devpilot.actions.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import org.jetbrains.annotations.NotNull;

public class ToolbarNewSessionAction extends AnAction {
    public ToolbarNewSessionAction() {
        super(DevPilotMessageBundle.get("devpilot.toolbarNewSessionAction.text"),
                DevPilotMessageBundle.get("devpilot.toolbarNewSessionAction.text"),
                AllIcons.General.Add);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevPilot");
        if (toolWindow == null) {
            return;
        }
        toolWindow.show();

        var service = project.getService(DevPilotChatToolWindowService.class);
        service.handleCreateNewSession();
        var editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null) {
            var editorInfo = new EditorInfo(editor);
            if (editorInfo.getSourceCode() != null) {
                var codeReference = CodeReferenceModel.getCodeRefFromEditor(editorInfo, null);
                service.referenceCode(codeReference);
            }
        }
    }
}