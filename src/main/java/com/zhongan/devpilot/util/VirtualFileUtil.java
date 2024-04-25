package com.zhongan.devpilot.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.IOException;
import java.nio.file.Path;

public class VirtualFileUtil {

    /**
     * create parent editor virtual file
     *
     * @return
     */
    public static VirtualFile createParentEditorVirtualFile() {
        // virtual file process,do not create new file in git or svn project, because it will be added to git or svn
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

    public static String getRelativeFilePath(Project project, VirtualFile virtualFile) {
        VcsRoot vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(virtualFile);
        if (vcsRoot != null) {
            return VfsUtilCore.getRelativePath(virtualFile, vcsRoot.getPath());
        }

        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile, false);
        if (module != null) {
            VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
            for (VirtualFile root : roots) {
                String relativePath = VfsUtilCore.getRelativePath(virtualFile, root);
                if (relativePath != null) {
                    return relativePath;
                }
            }
        }

        VirtualFile sourceRoot = ProjectFileIndex.getInstance(project).getSourceRootForFile(virtualFile);
        if (sourceRoot != null) {
            return VfsUtilCore.getRelativePath(virtualFile, sourceRoot);
        }

        return null;
    }

}