package com.zhongan.devpilot.completions.autoimport.handler;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.zhongan.devpilot.completions.autoimport.importer.DevpilotReferenceImporter;
import com.zhongan.devpilot.completions.autoimport.importer.java.DevpilotJavaReferenceImporter;
import com.zhongan.devpilot.completions.autoimport.importer.kotlin.DevpilotKotlinReferenceImporter;

import java.util.concurrent.atomic.AtomicBoolean;

public class AutoImportHandler {

    private  int startOffset;

    private  int endOffset;

    private Editor myEditor;

    private PsiFile myFile;


    public AutoImportHandler(int startOffset, int endOffset, Editor editor,PsiFile file) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.myEditor = editor;
        this.myFile = file;
    }

    public void invoke(){
        if (myEditor.isDisposed() || myFile.getProject().isDisposed()) return;
        VirtualFile virtualFile = myFile.getVirtualFile();
        DevpilotReferenceImporter importer = null;
        if(isJavaFile(virtualFile)){
            importer = new DevpilotJavaReferenceImporter(myEditor, myFile, startOffset, endOffset);
        }else if(isKotlinFile(virtualFile)){
            return;
        }else return;
        if(importer != null)importer.computeReferences();
    }


    public static boolean isJavaFile(VirtualFile file) {
        return file != null && file.getName().toLowerCase().endsWith(".java");
    }

    public static boolean isKotlinFile(VirtualFile file) {
        return file != null && file.getName().toLowerCase().endsWith(".kt");
    }
}
