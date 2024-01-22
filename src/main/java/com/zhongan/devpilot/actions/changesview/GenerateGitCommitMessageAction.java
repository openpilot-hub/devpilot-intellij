package com.zhongan.devpilot.actions.changesview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserBase;
import com.intellij.openapi.vcs.changes.ui.CommitDialogChangesBrowser;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.constant.PromptConst;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.DocumentUtil;
import com.zhongan.devpilot.util.MessageUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import static com.intellij.util.ObjectUtils.tryCast;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class GenerateGitCommitMessageAction extends AnAction {

    public GenerateGitCommitMessageAction() {
        super(DevPilotMessageBundle.get("devpilot.action.changesview.generateCommit"), DevPilotMessageBundle.get("devpilot.action.changesview.generateCommit"), DevPilotIcons.SYSTEM_ICON);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null || project.getBasePath() == null) {
            return;
        }

        try {
            String gitDiff = getGitDiff(project, getReferencedFilePaths(e));

            if (DocumentUtil.experienceEstimatedTokens(gitDiff) + DocumentUtil.experienceEstimatedTokens(PromptConst.GENERATE_COMMIT) > DefaultConst.TOKEN_MAX_LENGTH) {
                DevPilotNotification.warn(DevPilotMessageBundle.get("devpilot.changesview.tokens.estimation.overflow"));
            }

            var commitMessage = tryCast(e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL), CommitMessage.class);
            var editor = commitMessage != null ? commitMessage.getEditorField().getEditor() : null;
            if (editor != null) {
                ((EditorEx) editor).setCaretVisible(false);

                DevPilotMessage userMessage = MessageUtil.createUserMessage(gitDiff, "-1");
                DevPilotChatCompletionRequest devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
                devPilotChatCompletionRequest.getMessages().add(MessageUtil.createSystemMessage(PromptConst.GENERATE_COMMIT));
                devPilotChatCompletionRequest.getMessages().add(userMessage);
                devPilotChatCompletionRequest.setStream(Boolean.FALSE);

                var llmProvider = new LlmProviderFactory().getLlmProvider(project);
                DevPilotChatCompletionResponse result = llmProvider.chatCompletionSync(devPilotChatCompletionRequest);

                if (result.isSuccessful()) {
                    var application = ApplicationManager.getApplication();
                    application.invokeLater(() ->
                        application.runWriteAction(() ->
                            WriteCommandAction.runWriteCommandAction(project, () ->
                                editor.getDocument().setText(result.getContent()))));
                } else {
                    DevPilotNotification.warn(result.getContent());
                }

            }
        } catch (Exception ex) {
            DevPilotNotification.warn("Exception occurred while generating commit message");
        }
    }

    private @NotNull List<String> getReferencedFilePaths(AnActionEvent event) {
        var changesBrowserBase = event.getData(ChangesBrowserBase.DATA_KEY);
        if (changesBrowserBase == null) {
            return List.of();
        }

        var includedChanges = ((CommitDialogChangesBrowser) changesBrowserBase).getIncludedChanges();
        return includedChanges.stream()
            .filter(item -> item.getVirtualFile() != null)
            .map(item -> item.getVirtualFile().getPath())
            .collect(toList());
    }

    private Process createGitDiffProcess(String projectPath, List<String> filePaths) throws IOException {
        var command = new ArrayList<String>();
        command.add("git");
        command.add("diff");
        command.addAll(filePaths);

        var processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(projectPath));
        return processBuilder.start();
    }

    private String getGitDiff(Project project, List<String> filePaths) throws IOException {
        var process = createGitDiffProcess(project.getBasePath(), filePaths);
        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return reader.lines().collect(joining("\n"));
    }

}
