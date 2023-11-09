package com.zhongan.codeai.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.codeai.actions.notifications.CodeAINotification;
import com.zhongan.codeai.enums.EditorActionEnum;
import com.zhongan.codeai.enums.SessionTypeEnum;
import com.zhongan.codeai.gui.toolwindows.CodeAIChatToolWindowFactory;
import com.zhongan.codeai.gui.toolwindows.chat.CodeAIChatToolWindow;
import com.zhongan.codeai.gui.toolwindows.components.EditorInfo;
import com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState;
import com.zhongan.codeai.settings.state.LanguageSettingsState;
import com.zhongan.codeai.util.CodeAIMessageBundle;
import com.zhongan.codeai.util.DocumentUtil;
import com.zhongan.codeai.util.PerformanceCheckUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.Icon;

import static com.zhongan.codeai.enums.EditorActionEnum.EXPLAIN_THIS;
import static com.zhongan.codeai.enums.EditorActionEnum.FIX_THIS;
import static com.zhongan.codeai.enums.EditorActionEnum.GENERATE_COMMENTS;
import static com.zhongan.codeai.enums.EditorActionEnum.GENERATE_TESTS;
import static com.zhongan.codeai.enums.EditorActionEnum.PERFORMANCE_CHECK;
import static com.zhongan.codeai.enums.EditorActionEnum.REVIEW_CODE;
import static com.zhongan.codeai.util.Const.MAX_TOKEN_EXCEPTION_MSG;
import static com.zhongan.codeai.util.Const.TOKEN_MAX_LENGTH;

public class PopupMenuEditorActionGroupUtil {
    private static final Logger LOG = Logger.getInstance(PopupMenuEditorActionGroupUtil.class);

    private static final Map<String, Icon> ICONS = new LinkedHashMap<>(Map.of(
            PERFORMANCE_CHECK.getLabel(), AllIcons.Plugins.Updated,
            GENERATE_COMMENTS.getLabel(), AllIcons.Actions.InlayRenameInCommentsActive,
            GENERATE_TESTS.getLabel(), AllIcons.Modules.GeneratedTestRoot,
            FIX_THIS.getLabel(), AllIcons.Actions.QuickfixBulb,
            REVIEW_CODE.getLabel(), AllIcons.Actions.PreviewDetailsVertically,
            EXPLAIN_THIS.getLabel(), AllIcons.Actions.Preview));

    public static void refreshActions(Project project) {
        AnAction actionGroup = ActionManager.getInstance().getAction("com.zhongan.codeai.actions.editor.popupmenu.BasicEditorAction");
        if (actionGroup instanceof DefaultActionGroup) {
            DefaultActionGroup group = (DefaultActionGroup) actionGroup;
            group.removeAll();
            group.add(new NewChatAction());
            group.addSeparator();

            var defaultActions = EditorActionConfigurationState.getInstance().getDefaultActions();
            defaultActions.forEach((label, prompt) -> {
                var action = new BasicEditorAction(CodeAIMessageBundle.get(label), CodeAIMessageBundle.get(label), ICONS.getOrDefault(label, AllIcons.FileTypes.Unknown)) {
                    @Override
                    protected void actionPerformed(Project project, Editor editor, String selectedText) {
                        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Open Pilot");
                        toolWindow.show();
                        if (validateInput(selectedText, prompt)) {
                            CodeAINotification.info(CodeAIMessageBundle.get("codeai.notification.input.tooLong"));
                            return;
                        }

                        Consumer<String> callback = result -> {
                            if (validateResult(result)) {
                                CodeAINotification.info(CodeAIMessageBundle.get("codeai.notification.input.tooLong"));
                                return;
                            }

                            EditorActionEnum editorActionEnum = EditorActionEnum.getEnumByLabel(label);
                            if (Objects.isNull(editorActionEnum)) {
                                return;
                            }
                            switch (editorActionEnum) {
                                case PERFORMANCE_CHECK:
                                    //display result, and open diff window
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

                        CodeAIChatToolWindow codeAIChatToolWindow = CodeAIChatToolWindowFactory.getCodeAIChatToolWindow(project);
                        //right action clear session
                        codeAIChatToolWindow.addClearSessionInfo();
                        String newPrompt = prompt.replace("{{selectedCode}}", selectedText);
                        if (LanguageSettingsState.getInstance().getLanguageIndex() == 1) {
                            newPrompt = newPrompt + "Please response in Chinese ";
                        }
                        codeAIChatToolWindow.syncSendAndDisplay(SessionTypeEnum.MULTI_TURN.getCode(), EditorActionEnum.getEnumByLabel(label), newPrompt,
                                callback, editorInfo);
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
            actionManager.registerAction(actionId, action, PluginId.getId("com.zhongan.openPilot"));
        }
    }

    /**
     * check input length
     *
     * @return
     */
    private static boolean validateResult(String content) {
        return content.contains(MAX_TOKEN_EXCEPTION_MSG);
    }

    /**
     * check length of input rather than max limit
     * 1 token = 3 english character()
     *
     * @param content
     * @return
     */
    private static boolean validateInput(String content, String prompt) {
        //text too long, openai server always timeout
        if (content.length() + prompt.length() > TOKEN_MAX_LENGTH) {
            return true;
        }
        //valid chinese and english character length
        return DocumentUtil.getChineseCharCount(content + prompt) / 2 + DocumentUtil.getEnglishCharCount(content + prompt) / 4 > TOKEN_MAX_LENGTH;
    }

}
