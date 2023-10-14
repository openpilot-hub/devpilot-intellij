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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;

/**
 * Description
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 13:25 zhangzhisheng Exp $
 */
public class VirtualFileUtil {

    /**
     * create parent editor virtual file
     * @param document
     * @return
     */
    public static VirtualFile createParentEditorVirtualFile(Document document) {
        DiffContentFactory diffContentFactory = DiffContentFactory.getInstance();
        //virtual file process
        VirtualFile originalFile = FileDocumentManager.getInstance().getFile(document);
        //do not create new file in git or svn project, because it will be added to git or svn
        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(Path.
                        of(FilenameUtils.getPrefix(originalFile.getPath())));
    }

}