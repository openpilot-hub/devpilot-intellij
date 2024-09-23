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
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.constant.PromptConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.enums.UtFrameTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.provider.ut.UtFrameworkProvider;
import com.zhongan.devpilot.provider.ut.UtFrameworkProviderFactory;
import com.zhongan.devpilot.settings.actionconfiguration.EditorActionConfigurationState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.DocumentUtil;
import com.zhongan.devpilot.util.LanguageUtil;
import com.zhongan.devpilot.util.PsiElementUtils;
import com.zhongan.devpilot.util.PsiFileUtil;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.swing.Icon;

import static com.zhongan.devpilot.constant.PlaceholderConst.ADDITIONAL_MOCK_PROMPT;
import static com.zhongan.devpilot.constant.PlaceholderConst.ANSWER_LANGUAGE;
import static com.zhongan.devpilot.constant.PlaceholderConst.CLASS_FULL_NAME;
import static com.zhongan.devpilot.constant.PlaceholderConst.LANGUAGE;
import static com.zhongan.devpilot.constant.PlaceholderConst.MOCK_FRAMEWORK;
import static com.zhongan.devpilot.constant.PlaceholderConst.SELECTED_CODE;
import static com.zhongan.devpilot.constant.PlaceholderConst.TEST_FRAMEWORK;

public class PopupMenuEditorActionGroupUtil {

    private static final Map<String, Icon> ICONS = new LinkedHashMap<>(Map.of(
            EditorActionEnum.GENERATE_COMMENTS.getLabel(), AllIcons.Actions.InlayRenameInCommentsActive,
            EditorActionEnum.GENERATE_TESTS.getLabel(), AllIcons.Modules.GeneratedTestRoot,
            EditorActionEnum.FIX_CODE.getLabel(), AllIcons.Actions.QuickfixBulb,
            EditorActionEnum.EXPLAIN_CODE.getLabel(), AllIcons.Actions.Preview));

    public static void refreshActions(Project project) {
        AnAction actionGroup = ActionManager.getInstance().getAction("com.zhongan.devpilot.actions.editor.popupmenu.BasicEditorAction");
        if (actionGroup instanceof DefaultActionGroup) {
            DefaultActionGroup group = (DefaultActionGroup) actionGroup;
            group.removeAll();
            group.add(new NewChatAction());
            group.add(new ReferenceCodeAction());
            group.addSeparator();

            var defaultActions = EditorActionConfigurationState.getInstance().getDefaultActions();
            defaultActions.forEach((label) -> {
                var action = new BasicEditorAction(DevPilotMessageBundle.get(label), DevPilotMessageBundle.get(label), ICONS.getOrDefault(label, AllIcons.FileTypes.Unknown)) {
                    @Override
                    protected void actionPerformed(Project project, Editor editor, String selectedText, PsiElement psiElement, CodeReferenceModel codeReferenceModel) {
                        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevPilot");
                        toolWindow.show();
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
                        Map<String, String> data = new HashMap<>();
                        data.put(SELECTED_CODE, selectedText);

                        LanguageUtil.Language language = null;

                        var file = FileDocumentManager.getInstance().getFile(editor.getDocument());
                        if (file != null) {
                            language = LanguageUtil.getLanguageByExtension(file.getExtension());
                        }

                        if (language != null) {
                            data.put(LANGUAGE, language.getLanguageName());
                        }

                        EditorInfo editorInfo = new EditorInfo(editor);
                        if (editorActionEnum == EditorActionEnum.GENERATE_TESTS) {
                            if (language != null && language.isJvmPlatform()
                                    && PsiFileUtil.isCaretInWebClass(project, editor)) {
                                data.put(ADDITIONAL_MOCK_PROMPT, PromptConst.MOCK_WEB_MVC);
                            }
                            UtFrameworkProvider utFrameworkProvider = UtFrameworkProviderFactory.create(language);
                            if (utFrameworkProvider != null) {
                                UtFrameTypeEnum utFramework = utFrameworkProvider.getUTFramework(project, editor);
                                data.put(TEST_FRAMEWORK, utFramework.getUtFrameType());
                                data.put(MOCK_FRAMEWORK, utFramework.getMockFrameType());
                            }
                        }

                        if (language != null && "java".equalsIgnoreCase(language.getLanguageName())) {
                            if (psiElement != null) {
                                var fullClassName = PsiElementUtils.getFullClassName(psiElement);

                                if (fullClassName != null) {
                                    data.put(CLASS_FULL_NAME, fullClassName);
                                }
                            }
                        }

                        if (LanguageSettingsState.getInstance().getLanguageIndex() == 1) {
                            data.put(ANSWER_LANGUAGE, "zh_CN");
                        } else {
                            data.put(ANSWER_LANGUAGE, "en_US");
                        }

                        var service = project.getService(DevPilotChatToolWindowService.class);
                        var username = DevPilotLlmSettingsState.getInstance().getFullName();
                        service.clearRequestSession();

                        var showText = DevPilotMessageBundle.get(label);

                        if (codeReferenceModel == null) {
                            codeReferenceModel = CodeReferenceModel.getCodeRefFromEditor(editorInfo, editorActionEnum);
                        }

                        var codeMessage = MessageModel.buildCodeMessage(
                                UUID.randomUUID().toString(), System.currentTimeMillis(), showText, username, codeReferenceModel);

                        var psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, codeReferenceModel.getFileUrl());

                        if (psiJavaFile != null) {
                            data.putAll(
                                    Map.of(
                                            "imports", PsiElementUtils.getImportList(psiJavaFile),
                                            "package", psiJavaFile.getPackageName(),
                                            "fields", PsiElementUtils.getFieldList(psiJavaFile),
                                            "filePath", codeReferenceModel.getFileUrl()
                                    )
                            );
                        }

                        service.smartChat(SessionTypeEnum.MULTI_TURN.getCode(), editorActionEnum.name(), data, null, callback, codeMessage);
                    }
                };
                if (!label.equals(EditorActionEnum.COMMENT_METHOD.getLabel())) {
                    group.add(action);
                }
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
