package com.zhongan.devpilot.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.constant.PromptConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.settings.actionconfiguration.EditorActionConfigurationState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.DocumentUtil;
import com.zhongan.devpilot.util.PerformanceCheckUtils;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.swing.Icon;

public class PopupMenuEditorActionGroupUtil {

    private static final Map<String, Icon> ICONS = new LinkedHashMap<>(Map.of(
        EditorActionEnum.PERFORMANCE_CHECK.getLabel(), AllIcons.Plugins.Updated,
        EditorActionEnum.GENERATE_COMMENTS.getLabel(), AllIcons.Actions.InlayRenameInCommentsActive,
        EditorActionEnum.GENERATE_TESTS.getLabel(), AllIcons.Modules.GeneratedTestRoot,
        EditorActionEnum.FIX_THIS.getLabel(), AllIcons.Actions.QuickfixBulb,
        EditorActionEnum.REVIEW_CODE.getLabel(), AllIcons.Actions.PreviewDetailsVertically,
        EditorActionEnum.EXPLAIN_THIS.getLabel(), AllIcons.Actions.Preview));

    public static void refreshActions(Project project) {
        AnAction actionGroup = ActionManager.getInstance().getAction("com.zhongan.devpilot.actions.editor.popupmenu.BasicEditorAction");
        if (actionGroup instanceof DefaultActionGroup) {
            DefaultActionGroup group = (DefaultActionGroup) actionGroup;
            group.removeAll();
            group.add(new NewChatAction());
            group.addSeparator();

            var defaultActions = EditorActionConfigurationState.getInstance().getDefaultActions();
            defaultActions.forEach((label, prompt) -> {
                var action = new BasicEditorAction(DevPilotMessageBundle.get(label), DevPilotMessageBundle.get(label), ICONS.getOrDefault(label, AllIcons.FileTypes.Unknown)) {
                    @Override
                    protected void actionPerformed(Project project, Editor editor, String selectedText) {
                        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevPilot");
                        toolWindow.show();
                        if (isInputExceedLimit(selectedText, prompt)) {
                            DevPilotNotification.info(DevPilotMessageBundle.get("devpilot.notification.input.tooLong"));
                            return;
                        }

                        var editorActionEnum = EditorActionEnum.getEnumByLabel(label);
                        if (Objects.isNull(editorActionEnum)) {
                            return;
                        }

                        Consumer<String> callback = result -> {
                            DevPilotNotification.debug("result is -> [." + result + "].");
                            if (validateResult(result)) {
                                DevPilotNotification.info(DevPilotMessageBundle.get("devpilot.notification.input.tooLong"));
                                return;
                            }

                            switch (editorActionEnum) {
                                case PERFORMANCE_CHECK:
                                    // display result, and open diff window
                                    PerformanceCheckUtils.showDiffWindow(selectedText, project, editor);
                                    break;
                                case GENERATE_COMMENTS:
                                    DocumentUtil.diffCommentAndFormatWindow(project, editor, result);
                                    break;
                                default:
                                    break;
                            }
                        };

                        EditorInfo editorInfo = new EditorInfo(editor);

                        String newPrompt = prompt.replace("{{selectedCode}}", selectedText);
                        if (LanguageSettingsState.getInstance().getLanguageIndex() == 1) {
                            newPrompt = newPrompt + PromptConst.ANSWER_IN_CHINESE;
                        }

                        var service = project.getService(DevPilotChatToolWindowService.class);
                        var username = DevPilotLlmSettingsState.getInstance().getFullName();
                        service.clearRequestSession();

                        var showText = DevPilotMessageBundle.get(label);
                        var codeReference = new CodeReferenceModel(editorInfo.getFilePresentableUrl(),
                            editorInfo.getFileName(), editorInfo.getSelectedStartLine(), editorInfo.getSelectedEndLine(), editorActionEnum);

                        var codeMessage = MessageModel.buildCodeMessage(
                            UUID.randomUUID().toString(), System.currentTimeMillis(), showText, username, codeReference);

                        service.sendMessage(SessionTypeEnum.MULTI_TURN.getCode(), newPrompt, callback, codeMessage);
                    }
                };
                group.add(action);
            });
        }
    }

    public static void registerOrReplaceAction(AnAction action) {
        ActionManager actionManager = ActionManager.getInstance();
        var actionId = action.getTemplateText();
        if (actionManager.getAction(actionId) != null) {
            actionManager.replaceAction(actionId, action);
        } else {
            actionManager.registerAction(actionId, action, PluginId.getId("com.zhongan.devPilot"));
        }
    }

    /**
     * check input length
     *
     * @return
     */
    private static boolean validateResult(String content) {
        return content.contains(DefaultConst.MAX_TOKEN_EXCEPTION_MSG);
    }

    /**
     * check length of input rather than max limit
     * 1 token = 3 english character()
     *
     * @param content
     * @return
     */
    private static boolean isInputExceedLimit(String content, String prompt) {
        // text too long, openai server always timeout
        if (content.length() + prompt.length() > DefaultConst.TOKEN_MAX_LENGTH) {
            return true;
        }
        // valid chinese and english character length
        return DocumentUtil.experienceEstimatedTokens(content + prompt) > DefaultConst.TOKEN_MAX_LENGTH;
    }

}
