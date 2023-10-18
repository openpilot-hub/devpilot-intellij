 /*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.zhongan.codeai.util;

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
 import com.intellij.openapi.fileEditor.FileDocumentManager;
 import com.intellij.openapi.project.Project;
 import com.intellij.openapi.vfs.VirtualFile;
 import com.intellij.psi.PsiDocumentManager;
 import com.intellij.psi.codeStyle.CodeStyleManager;
 import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
 import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
 import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;

 import java.util.List;

 import static com.zhongan.codeai.util.DiffEditorUtils.getDiffContent;

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
            NO_PERFORMANCE_ISSUES_NULL,
            "no performance issues",
            "no apparent performance issues",
            "don't seem to be any performance issues",
            "don't appear to be any performance issues"
            );

    private static final String CUSTOM_PROMPT = "Giving the code above, please fix any performance issues," +
            "if there are does not have any performance issues, please only return null, " +
            "otherwise, please only return the fixed code without any description.";

    /**
     * get performance check result
     * @param selectedText
     * @param project
     * @param editor
     * @return
     */
    public static String getChatCompletionResult(String selectedText, Project project, Editor editor) {
        CodeAIMessage codeAIMessage = new CodeAIMessage();
        codeAIMessage.setRole("user");
        codeAIMessage.setContent(selectedText + CUSTOM_PROMPT);
        CodeAIChatCompletionRequest request = new CodeAIChatCompletionRequest();
        request.setMessages(java.util.List.of(codeAIMessage));
        final String code = new LlmProviderFactory().getLlmProvider(project).chatCompletion(request);
        //if no performance issues, return original code
        for (String noPerformanceIssue : NO_PERFORMANCE_ISSUES) {
            if(code.contains(noPerformanceIssue)) {
                return selectedText;
            }
        }
        return code;
    }

    /**
     *
     * @param project
     * @param editor
     * @param originalFile
     * @param replaceDocument
     */
    public static void showDiff(Project project, Editor editor, VirtualFile originalFile, Document replaceDocument) {
        DiffContentFactory diffContentFactory = DiffContentFactory.getInstance();
        DiffContent replaceContent = getDiffContent(diffContentFactory, project, replaceDocument);
        DiffContent originalContent = getDiffContent(diffContentFactory,project, editor.getDocument());
        DiffRequest diffRequest = new SimpleDiffRequest("Code AI: Diff view",
                replaceContent, originalContent, "Code AI suggested code", originalFile.getName()+"(original code)");
        DiffManager diffManager = DiffManager.getInstance();
        diffManager.showDiff(project, diffRequest);
    }

    /**
     *  display result, and open diff window
     * @param selectedText
     * @param project
     * @param editor
     * @param replaceFile
     */
    public static void showDiffWindow(String selectedText, Project project, Editor editor, VirtualFile replaceFile) {
        final String code = getChatCompletionResult(selectedText, project, editor);
        Document replaceDocument = FileDocumentManager.getInstance().getDocument(replaceFile);
        var selectionModel = editor.getSelectionModel();
        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
            replaceDocument.setText(editor.getDocument().getText());
            replaceDocument.setReadOnly(false);
            replaceDocument.replaceString(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), code);

            //auto code format
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            codeStyleManager.reformatText(PsiDocumentManager.getInstance(project).getPsiFile(replaceDocument),
                    selectionModel.getSelectionStart(), selectionModel.getSelectionStart() + code.length());
        }));
        //todo auto format code
        showDiff(project, editor, FileDocumentManager.getInstance().getFile(editor.getDocument()), replaceDocument);
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