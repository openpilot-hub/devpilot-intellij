package com.zhongan.devpilot.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.MessageModel;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ReferenceChatAction extends AnAction {

    public ReferenceChatAction() {
        super(DevPilotMessageBundle.get("devpilot.action.reference.chat"), DevPilotMessageBundle.get("devpilot.action.reference.chat"), AllIcons.Actions.Find);
        PopupMenuEditorActionGroupUtil.registerOrReplaceAction(this);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var project = event.getProject();
        if (project == null) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevPilot");
        if (toolWindow == null) {
            return;
        }
        toolWindow.show();

        var editor = event.getData(PlatformDataKeys.EDITOR);

        if (editor == null) {
            return;
        }

        var editorInfo = new EditorInfo(editor);
        var codeReference = new CodeReferenceModel(editorInfo.getFilePresentableUrl(),
                editorInfo.getFileName(), editorInfo.getSelectedStartLine(), editorInfo.getSelectedEndLine(), null);

        var service = project.getService(DevPilotChatToolWindowService.class);
        var username = DevPilotLlmSettingsState.getInstance().getFullName();
        service.clearRequestSession();

    }

}
