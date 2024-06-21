package com.zhongan.devpilot.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
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
import com.zhongan.devpilot.util.LanguageUtil;
import com.zhongan.devpilot.util.PromptTemplate;
import com.zhongan.devpilot.util.PsiFileUtil;
import com.zhongan.devpilot.util.TokenUtils;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import javax.swing.Icon;

import static com.zhongan.devpilot.constant.PlaceholderConst.ADDITIONAL_MOCK_PROMPT;
import static com.zhongan.devpilot.constant.PlaceholderConst.LANGUAGE;
import static com.zhongan.devpilot.constant.PlaceholderConst.MOCK_FRAMEWORK;
import static com.zhongan.devpilot.constant.PlaceholderConst.SELECTED_CODE;
import static com.zhongan.devpilot.constant.PlaceholderConst.TEST_FRAMEWORK;

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
                        if (TokenUtils.isInputExceedLimit(selectedText, prompt)) {
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

                            if (editorActionEnum == EditorActionEnum.GENERATE_COMMENTS) {
                                DocumentUtil.diffCommentAndFormatWindow(project, editor, result);
                            }
                        };

                        EditorInfo editorInfo = new EditorInfo(editor);
                        PromptTemplate promptTemplate = PromptTemplate.of(prompt);
                        promptTemplate.setVariable(SELECTED_CODE, selectedText);
                        if (editorActionEnum == EditorActionEnum.GENERATE_TESTS) {
                            Optional.ofNullable(FileDocumentManager.getInstance().getFile(editor.getDocument()))
                                    .map(vFile -> LanguageUtil.getLanguageByExtension(vFile.getExtension()))
                                    .ifPresent(language -> {
                                        promptTemplate.setVariable(LANGUAGE, language.getLanguageName());
                                        promptTemplate.setVariable(TEST_FRAMEWORK, language.getDefaultTestFramework());
                                        promptTemplate.setVariable(MOCK_FRAMEWORK, language.getDefaultMockFramework());
                                        if (language.isJvmPlatform() && PsiFileUtil.isCaretInWebClass(project, editor)) {
                                            promptTemplate.setVariable(ADDITIONAL_MOCK_PROMPT, PromptConst.MOCK_WEB_MVC);
                                        }
                                    });
                        }
                        if (LanguageSettingsState.getInstance().getLanguageIndex() == 1
                                && editorActionEnum != EditorActionEnum.GENERATE_COMMENTS) {
                            promptTemplate.appendLast(PromptConst.ANSWER_IN_CHINESE);
                        }

                        var service = project.getService(DevPilotChatToolWindowService.class);
                        var username = DevPilotLlmSettingsState.getInstance().getFullName();
                        service.clearRequestSession();

                        var showText = DevPilotMessageBundle.get(label);
                        var codeReference = new CodeReferenceModel(editorInfo.getFilePresentableUrl(),
                            editorInfo.getFileName(), editorInfo.getSelectedStartLine(), editorInfo.getSelectedEndLine(), editorActionEnum);

                        var codeMessage = MessageModel.buildCodeMessage(
                            UUID.randomUUID().toString(), System.currentTimeMillis(), showText, username, codeReference);

                        service.sendMessage(SessionTypeEnum.MULTI_TURN.getCode(), promptTemplate.getPrompt(), callback, codeMessage);
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
    public static boolean validateResult(String content) {
        return content.contains(DefaultConst.GPT_35_MAX_TOKEN_EXCEPTION_MSG);
    }

}
