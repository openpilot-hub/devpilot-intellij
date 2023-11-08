package com.zhongan.codeai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.zhongan.codeai.actions.editor.popupmenu.entity.PerformanceCheckResponse;
import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Description
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 10:27 zhangzhisheng Exp $
 */
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
        CodeAIMessage codeAIMessage = new CodeAIMessage();
        codeAIMessage.setRole("user");
        codeAIMessage.setContent(selectedText + "\n" + CUSTOM_PROMPT);
        CodeAIChatCompletionRequest request = new CodeAIChatCompletionRequest();
        //list content support update
        request.setMessages(new ArrayList<>() {
            {
                add(codeAIMessage);
            }
        });
        final String response = new LlmProviderFactory().getLlmProvider(project).chatCompletion(request);
        try {
            PerformanceCheckResponse performanceCheckResponse = objectMapper.readValue(response, PerformanceCheckResponse.class);
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
            //return original code if return result is error
            return selectedText;
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
