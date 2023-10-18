package com.zhongan.codeai.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

/**
 * Description
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 13:25 zhangzhisheng Exp $
 */
public class VirtualFileUtil {

    /**
     * create parent editor virtual file
     *
     * @param document
     * @return
     */
    public static VirtualFile createParentEditorVirtualFile(Document document) {
        //virtual file process
        VirtualFile originalFile = FileDocumentManager.getInstance().getFile(document);
        //do not create new file in git or svn project, because it will be added to git or svn
        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(Path.
            of(FilenameUtils.getPrefix(originalFile.getPath())));
    }

}