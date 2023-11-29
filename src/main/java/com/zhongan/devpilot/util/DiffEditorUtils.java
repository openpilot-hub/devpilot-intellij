package com.zhongan.devpilot.util;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class DiffEditorUtils {

    /**
     * get diffContent
     *
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
     *
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