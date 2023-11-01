package com.zhongan.codeai.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.IOException;
import java.nio.file.Path;

/**
 * VirtualFileUtil
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 13:25 zhangzhisheng Exp $
 */
public class VirtualFileUtil {

    /**
     * create parent editor virtual file
     *
     * @return
     */
    public static VirtualFile createParentEditorVirtualFile() {
        //virtual file process,do not create new file in git or svn project, because it will be added to git or svn
        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(Path.of(System.getProperty("user.home")));
    }

    /**
     * create virtual replace file
     * @param editor
     * @param project
     * @return
     */
    public static VirtualFile createVirtualReplaceFile(Editor editor, Project project) {
        // process create parent virtual file can not access exception
        return ApplicationManager.getApplication().runWriteAction((Computable<VirtualFile>) () -> {
            VirtualFile createdFile = null;
            try {
                createdFile = createParentEditorVirtualFile().createChildData(project,
                        System.currentTimeMillis() + "." + FileDocumentManager.getInstance().
                                getFile(editor.getDocument()).getExtension());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return createdFile;
        });
    }

}