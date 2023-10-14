package com.zhongan.codeai.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.zhongan.codeai.gui.toolwindows.CodeAIChatToolWindowFactory;
import com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState;

import javax.swing.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.zhongan.codeai.util.PerformanceCheckUtils.*;
import static com.zhongan.codeai.util.VirtualFileUtil.createParentEditorVirtualFile;

public class PopupMenuEditorActionGroupUtil {
    private static final Logger LOG = Logger.getInstance(PopupMenuEditorActionGroupUtil.class);
    private static final Map<String, Icon> ICONS = new LinkedHashMap<>(Map.of(
            "Performance Check", AllIcons.Plugins.Updated,
            "Generate Comments", AllIcons.Actions.InlayRenameInCommentsActive,
            "Generate Tests", AllIcons.Modules.GeneratedTestRoot,
            "Generate Docs", AllIcons.Gutter.JavadocEdit,
            "Fix This", AllIcons.Actions.QuickfixBulb,
            "Translate This", AllIcons.Actions.QuickfixBulb,
            "Explain This", AllIcons.Actions.Preview));

    public static void refreshActions(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeAI");
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
                        toolWindow.show();
                        CodeAIChatToolWindowFactory.codeAIChatToolWindow.syncSendAndDisplay(prompt.replace("{{selectedCode}}", selectedText));
                        switch (label) {
                            case "Performance Check":
                                //display result, and open diff window
                                VirtualFile originalFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
                                //virtual file process
                                VirtualFile replaceFile = createVirtualReplaceFile(editor);
                                //DiffContent process
                                showDiffWindow(prompt, selectedText, project, editor, originalFile, replaceFile);
                                break;
                            case "Generate Comments":
                                //todo 插入注释
                                break;
                            default:
                                LOG.error("could not trigger action {}", label);
                        }
//                        CodeAINotification.info(label + ": " + prompt + ": " + selectedText + ":result:" + result);
                    }

                    /**
                     * create virtual replace file
                     * @param editor
                     * @return
                     */
                    private VirtualFile createVirtualReplaceFile(Editor editor) {
                        VirtualFile originalFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
                        // process create parent virtualfile can not access excetion
                        return ApplicationManager.getApplication().runWriteAction((Computable<VirtualFile>) () -> {
                            VirtualFile createdFile = null;
                            try {
                                createdFile = createParentEditorVirtualFile(editor.getDocument()).createChildData(this,
                                        System.currentTimeMillis() + originalFile.getExtension());
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

    /**
     *  display result, and open diff window
     * @param prompt
     * @param selectedText
     * @param project
     * @param editor
     * @param originalFile
     * @param newFile
     */
    private static void showDiffWindow(String prompt, String selectedText, Project project, Editor editor, VirtualFile originalFile, VirtualFile newFile) {
        final String code = getChatCompletionResult(selectedText, project, editor);
        var selectionModel = editor.getSelectionModel();
        Document replaceDocument = FileDocumentManager.getInstance().getDocument(newFile);
        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
            replaceDocument.setText(editor.getDocument().getText());
            replaceDocument.setReadOnly(false);
            replaceDocument.replaceString(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), code);
        }));
        showDiff(project, editor, originalFile, replaceDocument);
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

}
