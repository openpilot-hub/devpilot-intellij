package com.zhongan.devpilot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.PerformanceCheckResponse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class PerformanceCheckUtils {

    public static final String NO_PERFORMANCE_ISSUES_DESC = "There don't appear to be any performance issues with the given code.";

    public static final String NO_PERFORMANCE_ISSUES_NULL = "null";

    public static final List<String> NO_PERFORMANCE_ISSUES = Lists.newArrayList(NO_PERFORMANCE_ISSUES_DESC,
            NO_PERFORMANCE_ISSUES_NULL);

    private final static ObjectMapper objectMapper = new ObjectMapper();

    private static final String CUSTOM_PROMPT = "Please optimize the code above for performance. " +
            "Provide two outputs: one as 'null' indicating no performance issues, " +
            "and the other as the code after performance optimization, " +
            "returned in JSON format with the key 'rewriteCode'.";

    /**
     * get performance check result
     *
     * @param selectedText
     * @param project
     * @return
     */
    public static String getChatCompletionResult(String selectedText, Project project) {
        DevPilotMessage devPilotMessage = new DevPilotMessage();
        devPilotMessage.setRole("user");
        devPilotMessage.setContent(selectedText + "\n" + CUSTOM_PROMPT);
        DevPilotChatCompletionRequest request = new DevPilotChatCompletionRequest();
        // list content support update
        request.setMessages(new ArrayList<>() {{ add(devPilotMessage); }});

        final DevPilotChatCompletionResponse response = new LlmProviderFactory().getLlmProvider(project).chatCompletionSync(request);
        try {
            DevPilotNotification.debug("Getting PerformanceCheckResponse is [" + response.isSuccessful() + "], content is [" + response.getContent() + "].");
            if (!response.isSuccessful()) {
                return selectedText;
            }
            PerformanceCheckResponse performanceCheckResponse = objectMapper.readValue(response.getContent(), PerformanceCheckResponse.class);
            if (StringUtils.isEmpty(performanceCheckResponse.getRewriteCode())) {
                return selectedText;
            }
            for (String noPerformanceIssue : NO_PERFORMANCE_ISSUES) {
                if (performanceCheckResponse.getRewriteCode().contains(noPerformanceIssue)) {
                    return selectedText;
                }
            }
            return performanceCheckResponse.getRewriteCode();
        } catch (Exception e) {
            // return original code if return result is error
            return selectedText;
        }
    }

    /**
     * show diff windows
     *
     * @param project
     * @param editor
     * @param originalFile
     * @param replaceDocument
     */
    public static void showDiff(Project project, Editor editor, VirtualFile originalFile, Document replaceDocument) {
        DiffContentFactory diffContentFactory = DiffContentFactory.getInstance();
        DiffContent replaceContent = DiffEditorUtils.getDiffContent(diffContentFactory, project, replaceDocument);
        DiffContent originalContent = DiffEditorUtils.getDiffContent(diffContentFactory, project, editor.getDocument());
        DiffRequest diffRequest = new SimpleDiffRequest("Dev Pilot: Diff view",
            replaceContent, originalContent, "Dev Pilot suggested code", originalFile.getName() + "(original code)");
        DiffManager diffManager = DiffManager.getInstance();
        diffManager.showDiff(project, diffRequest);
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorReleased(@NotNull EditorFactoryEvent event) {
                deleteVirtualFileIfNeeded(FileDocumentManager.getInstance().getFile(replaceDocument), project);
            }
        }, () -> {
        });
    }

    private static void deleteVirtualFileIfNeeded(VirtualFile virtualFile, Project project) {
        if (virtualFile != null && virtualFile.exists()) {
            ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    virtualFile.delete(null);
                } catch (Exception e) {

                }
            }));
        }
    }

    /**
     * display result, and open diff window
     *
     * @param selectedText
     * @param project
     * @param editor
     */
    public static void showDiffWindow(String selectedText, Project project, Editor editor) {
        final String code = getChatCompletionResult(selectedText, project);
        if (code.equals(selectedText)) {
            return;
        }
        DocumentUtil.diffCommentAndFormatWindow(project, editor, code);
    }

    private static String getCode(String result) {
        StringBuilder codeSnippet = new StringBuilder();
        int startIndex = result.indexOf("```");
        while (startIndex != -1) {
            int endIndex = result.indexOf("```", startIndex + 3);
            if (endIndex != -1) {
                codeSnippet.append(result.substring(startIndex + 3, endIndex)).append("\n");
                startIndex = result.indexOf("```", endIndex + 3);
            } else {
                break;
            }
        }
        return codeSnippet.toString();
    }

}