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
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Description
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 16:58 zhangzhisheng Exp $
 */
public class DocumentUtil {

    public static final List<String> CODE_FLAGS = Lists.newArrayList("(?s)```[\\s\\S]*?```", "\\b(public|class|def|function|var|let)\\b");

    /**
     * insert comment and format code
     * @param project
     * @param editor
     * @param result
     */
    public static void insertCommentAndFormat(Project project, Editor editor, String result) {
        final boolean containCode = containsCode(result);
        final StringBuilder text = new StringBuilder(result);
        //check result have code block, if not, add code block
        if (!containCode) {
            text.append(editor.getSelectionModel().getSelectedText()).append("\n").toString();
        }
        //insert comment
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            int caretOffset = editor.getSelectionModel().getSelectionStart();
            document.replaceString(editor.getSelectionModel().getSelectionStart(),
                    editor.getSelectionModel().getSelectionEnd(),
                    text.toString());
            // format code
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            codeStyleManager.reformatText(PsiManager.getInstance(project).findFile(file),
                    caretOffset, caretOffset + text.toString().length());
        });
    }
    public static boolean containsCode(String content) {
        for (String regex : CODE_FLAGS) {
            Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(content).find();
        }
        return false;
    }


}