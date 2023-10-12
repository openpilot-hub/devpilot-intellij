package com.zhongan.codeai.actions.editor.popupmenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.zhongan.codeai.actions.notifications.CodeAINotification;
import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;
import com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class PopupMenuEditorActionGroupUtil {

    private static final Map<String, Icon> ICONS = new LinkedHashMap<>(Map.of(
        "Performance Check", AllIcons.Plugins.Updated,
        "Generate Comments", AllIcons.Actions.InlayRenameInCommentsActive,
        "Generate Tests", AllIcons.Modules.GeneratedTestRoot,
        "Generate Docs", AllIcons.Gutter.JavadocEdit,
        "Fix This", AllIcons.Actions.QuickfixBulb,
        "Translate This", AllIcons.Actions.QuickfixBulb,
        "Explain This", AllIcons.Actions.Preview));

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
                        //todo 代码写入窗口
                        if("Performance Check".equals(label)){
                        }
                        CodeAIMessage codeAIMessage = new CodeAIMessage();
                        codeAIMessage.setRole("user");
                        codeAIMessage.setContent(prompt.replace("{{selectedCode}}", selectedText));

                        CodeAIChatCompletionRequest request = new CodeAIChatCompletionRequest();
                        request.setMessages(java.util.List.of(codeAIMessage));
                        String result = new LlmProviderFactory().getLlmProvider(project).chatCompletion(request);
                        CodeAINotification.info(label + ": " + prompt + ": " + selectedText + ":result:" + result);
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

}
