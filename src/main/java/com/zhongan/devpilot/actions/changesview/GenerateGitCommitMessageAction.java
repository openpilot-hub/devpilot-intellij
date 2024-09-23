package com.zhongan.devpilot.actions.changesview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.MessageUtil;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

public class GenerateGitCommitMessageAction extends AnAction {

    private static final Logger log = Logger.getInstance(GenerateGitCommitMessageAction.class);

    public GenerateGitCommitMessageAction() {
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Presentation presentation = e.getPresentation();
        presentation.setText(DevPilotMessageBundle.get("devpilot.action.changesview.generateCommit"));
        presentation.setDescription(DevPilotMessageBundle.get("devpilot.action.changesview.generateCommit"));
        presentation.setIcon(DevPilotIcons.SYSTEM_ICON);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null || project.getBasePath() == null) {
            return;
        }

        try {
            List<Change> changeList = getReferencedFilePaths(e);
            String diff = getGitDiff(e, changeList);
            if (StringUtils.isEmpty(diff)) {
                DevPilotNotification.info("no changes selected");
                return;
            }
            var commitMessage = ObjectUtils.tryCast(e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL), CommitMessage.class);
            var editor = commitMessage != null ? commitMessage.getEditorField().getEditor() : null;
            ApplicationManager.getApplication().invokeLater(() ->
                    ApplicationManager.getApplication().runWriteAction(() ->
                            WriteCommandAction.runWriteCommandAction(project, () -> {
                                if (editor != null) {
                                    editor.getDocument().setText(" ");
                                }
                            })));
            generateCommitMessage(project, diff, editor);
        } catch (Exception ex) {
            DevPilotNotification.warn("Exception occurred while generating commit message");
        }
    }

    private void generateCommitMessage(Project project, String diff, Editor editor) {
        new Task.Backgroundable(project, DevPilotMessageBundle.get("devpilot.commit.tip"), true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                if (editor != null) {
                    ((EditorEx) editor).setCaretVisible(false);
                    DevPilotChatCompletionRequest devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
                    devPilotChatCompletionRequest.setVersion("V240923");
                    devPilotChatCompletionRequest.getMessages().add(MessageUtil.createPromptMessage("-1", "GENERATE_COMMIT", Map.of("locale", getLocale(), "diff", diff)));
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
            }
        }.queue();
    }

    private @NotNull List<Change> getReferencedFilePaths(AnActionEvent event) {

        var workflowHandler = event.getDataContext().getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        List<Change> changeList = new ArrayList<>();
        if (workflowHandler instanceof AbstractCommitWorkflowHandler) {
            List<Change> includedChanges = ((AbstractCommitWorkflowHandler<?, ?>) workflowHandler).getUi().getIncludedChanges();
            if (!includedChanges.isEmpty()) {
                changeList.addAll(includedChanges);
            }
            List<FilePath> filePaths = ((AbstractCommitWorkflowHandler<?, ?>) workflowHandler).getUi().getIncludedUnversionedFiles();
            if (!filePaths.isEmpty()) {
                for (FilePath filePath : filePaths) {
                    Change change = new Change(null, new CurrentContentRevision(filePath));
                    changeList.add(change);
                }
            }
        }
        return changeList;
    }

    private String getGitDiff(AnActionEvent event, List<Change> includedChanges) {
        if (includedChanges.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        Project project = event.getProject();
        if (project == null) {
            return null;
        }
        GitRepositoryManager gitRepositoryManager = GitRepositoryManager.getInstance(project);
        Map<GitRepository, List<Change>> changesByRepository = new HashMap<>();
        for (Change change : includedChanges) {
            VirtualFile file = change.getVirtualFile();
            if (file != null) {
                GitRepository repository = gitRepositoryManager.getRepositoryForFileQuick(file);
                changesByRepository.computeIfAbsent(repository, k -> new ArrayList<>()).add(change);
            }
        }

        changesByRepository.forEach((gitRepository, changes) -> {
            if (gitRepository != null) {
                try {
                    if (project.getBasePath() == null) {
                        return;
                    }
                    List<FilePatch> filePatches = IdeaTextPatchBuilder.buildPatch(project, changes, Path.of(project.getBasePath()), false, true);
                    StringWriter stringWriter = new StringWriter();
                    stringWriter.write("Repository: " + gitRepository.getRoot().getPath() + "\n");

                    UnifiedDiffWriter.write(project, filePatches, stringWriter, "\n", null);

                    result.append(stringWriter);
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }
        });
        return result.toString();
    }

    public static String getLocale() {
        Integer languageIndex = LanguageSettingsState.getInstance().getLanguageIndex();
        Locale locale = languageIndex == 0 ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
        return locale.getDisplayLanguage();
    }

}
