package com.zhongan.codeai.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.codeai.actions.editor.EditorActionEnum;
import com.zhongan.codeai.actions.notifications.CodeAINotification;
import com.zhongan.codeai.gui.toolwindows.CodeAIChatToolWindowFactory;
import com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState;
import com.zhongan.codeai.util.DocumentUtil;
import com.zhongan.codeai.util.PerformanceCheckUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.Icon;

import static com.zhongan.codeai.actions.editor.EditorActionEnum.EXPLAIN_THIS;
import static com.zhongan.codeai.actions.editor.EditorActionEnum.FIX_THIS;
import static com.zhongan.codeai.actions.editor.EditorActionEnum.GENERATE_COMMENTS;
import static com.zhongan.codeai.actions.editor.EditorActionEnum.GENERATE_DOCS;
import static com.zhongan.codeai.actions.editor.EditorActionEnum.GENERATE_TESTS;
import static com.zhongan.codeai.actions.editor.EditorActionEnum.PERFORMANCE_CHECK;
import static com.zhongan.codeai.actions.editor.EditorActionEnum.REVIEW_CODE;
import static com.zhongan.codeai.util.Const.MAX_TOKEN_EXCEPTION_MSG;
import static com.zhongan.codeai.util.Const.TOKEN_MAX_LENGTH;
import static com.zhongan.codeai.util.VirtualFileUtil.createParentEditorVirtualFile;

public class PopupMenuEditorActionGroupUtil {
    private static final Logger LOG = Logger.getInstance(PopupMenuEditorActionGroupUtil.class);

    private static final Map<String, Icon> ICONS = new LinkedHashMap<>(Map.of(
        PERFORMANCE_CHECK.getLabel(), AllIcons.Plugins.Updated,
        GENERATE_COMMENTS.getLabel(), AllIcons.Actions.InlayRenameInCommentsActive,
        GENERATE_TESTS.getLabel(), AllIcons.Modules.GeneratedTestRoot,
        GENERATE_DOCS.getLabel(), AllIcons.Gutter.JavadocEdit,
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
                var action = new BasicEditorAction(label, label, ICONS.getOrDefault(label, AllIcons.FileTypes.Unknown)) {
                    @Override
                    protected void actionPerformed(Project project, Editor editor, String selectedText) {
                        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Open Pilot");
                        toolWindow.show();
                        if (validateInput(selectedText, prompt)) {
                            CodeAINotification.info("The input length is too long, please reduce the length of the messages.");
                            return;
                        }

                        Consumer<String> callback = result -> {
                            if (validateResult(result)) {
                                CodeAINotification.info("The input length is too long, please reduce the length of the messages.");
                                return;
                            }

                            EditorActionEnum editorActionEnum = EditorActionEnum.getEnumByLabel(label);
                            if (Objects.isNull(editorActionEnum)) {
                                return;
                            }
                            switch (editorActionEnum) {
                                case PERFORMANCE_CHECK:
                                    //display result, and open diff window
                                    PerformanceCheckUtils.showDiffWindow(selectedText, project, editor, createVirtualReplaceFile(editor));
                                    break;
                                case GENERATE_COMMENTS:
                                    DocumentUtil.insertCommentAndFormat(project, editor, result);
                                    break;
                                default:
                                    break;
                            }
                        };

                        CodeAIChatToolWindowFactory.getCodeAIChatToolWindow(project)
                                .syncSendAndDisplay(prompt.replace("{{selectedCode}}", selectedText), callback);
                    }

                    /**
                     * create virtual replace file
                     * @param editor
                     * @return
                     */
                    private VirtualFile createVirtualReplaceFile(Editor editor) {
                        // process create parent virtual file can not access exception
                        return ApplicationManager.getApplication().runWriteAction((Computable<VirtualFile>) () -> {
                            VirtualFile createdFile = null;
                            try {
                                createdFile = createParentEditorVirtualFile(editor.getDocument()).createChildData(this,
                                    System.currentTimeMillis() + "." + FileDocumentManager.getInstance().
                                        getFile(editor.getDocument()).getExtension());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return createdFile;
                        });
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
            actionManager.registerAction(actionId, action, PluginId.getId("com.zhongan.codeAI"));
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
