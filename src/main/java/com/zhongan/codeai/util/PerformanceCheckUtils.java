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

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;

import static com.zhongan.codeai.util.DiffEditorUtils.getDiffContent;

/**
 * Description
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 10:27 zhangzhisheng Exp $
 */
public class PerformanceCheckUtils {

    public static final String NO_PERFORMANCE_ISSUES_DESC = "There don't appear to be any performance issues with the given code.";
    public static final String NO_PERFORMANCE_ISSUES_EMPTY = "empty";

    private static final String CUSTOM_PROMPT = "Giving the code above, please fix any performance issues.\n " +
            "If there are does not have any performance issues, please just return empty; \n" +
            "Otherwise, please only return the code without any description.";

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
        String code = new LlmProviderFactory().getLlmProvider(project).chatCompletion(request);
        //if code is empty, return original code
        if(NO_PERFORMANCE_ISSUES_DESC.equals(code) || NO_PERFORMANCE_ISSUES_EMPTY.equals(code)) {
            code = editor.getDocument().getText();
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
        DiffContent textContent = getDiffContent(diffContentFactory, project, replaceDocument);
        DiffContent originalContent = getDiffContent(diffContentFactory,project, editor.getDocument());
        DiffRequest diffRequest = new SimpleDiffRequest("Code AI: Diff view",
                textContent, originalContent, "Code AI suggested code", originalFile.getName()+"(original code)");
        DiffManager diffManager = DiffManager.getInstance();
        diffManager.showDiff(project, diffRequest);
    }

}