package com.zhongan.devpilot.actions.editor;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import static com.zhongan.devpilot.actions.editor.popupmenu.PopupMenuEditorActionGroupUtil.validateResult;

public abstract class SelectedCodeGenerateBaseAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevPilot");
        if (toolWindow != null) {
            toolWindow.show();
        }

        Consumer<String> callback = result -> {
            if (validateResult(result)) {
                DevPilotNotification.info(DevPilotMessageBundle.get("devpilot.notification.input.tooLong"));
            }
            handleValidResult(result);
        };

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        String selectedText = editor.getSelectionModel().getSelectedText();
        String prompt = getPrompt().replace("{{selectedCode}}", selectedText);

        EditorInfo editorInfo = new EditorInfo(editor);
        var service = project.getService(DevPilotChatToolWindowService.class);
        var username = DevPilotLlmSettingsState.getInstance().getFullName();
        service.clearRequestSession();

        var showText = getShowText();
        var codeReference = new CodeReferenceModel(editorInfo.getFilePresentableUrl(),
                editorInfo.getFileName(), editorInfo.getSelectedStartLine(), editorInfo.getSelectedEndLine(), getEditorActionEnum());

        var codeMessage = MessageModel.buildCodeMessage(
                UUID.randomUUID().toString(), System.currentTimeMillis(), showText, username, codeReference);

        service.sendMessage(SessionTypeEnum.MULTI_TURN.getCode(), prompt, callback, codeMessage);
    }

    protected abstract String getPrompt();

    protected abstract EditorActionEnum getEditorActionEnum();

    protected abstract String getShowText();

    protected abstract void handleValidResult(String result);
}
