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
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Description
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 10:34 zhangzhisheng Exp $
 */
public class DiffEditorUtils {

    /**
     * get diffContent
     * @param diffContentFactory
     * @param project
     * @param document
     * @return
     */
    public static DiffContent getDiffContent(DiffContentFactory diffContentFactory, Project project, Document document) {
        try {
            return diffContentFactory.create(project, FileDocumentManager.getInstance().getFile(document));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * get diffContent
     * @param diffContentFactory
     * @param project
     * @param file
     * @return
     */
    public static DiffContent getDiffContent(DiffContentFactory diffContentFactory, Project project, VirtualFile file) {
        try {
            return diffContentFactory.create(project, file);
        } catch (Exception e) {
            return null;
        }
    }
}