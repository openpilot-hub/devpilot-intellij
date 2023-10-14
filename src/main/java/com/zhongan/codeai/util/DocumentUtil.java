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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * Description
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 16:58 zhangzhisheng Exp $
 */
public class DocumentUtil {

    /**
     * insert comment and format code
     * @param project
     * @param editor
     * @param result
     */
    public static void insertCommentAndFormat(Project project, Editor editor, String result) {
        //insert comment
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            int caretOffset = editor.getSelectionModel().getSelectionStart();
            document.replaceString(editor.getSelectionModel().getSelectionStart(),
                    editor.getSelectionModel().getSelectionEnd(),
                    result+"\n");
            // format code
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            codeStyleManager.reformatText(PsiManager.getInstance(project).findFile(file),
                    caretOffset, caretOffset + result.length());
        });
    }
}