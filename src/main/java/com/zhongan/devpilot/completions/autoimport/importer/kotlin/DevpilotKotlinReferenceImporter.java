package com.zhongan.devpilot.completions.autoimport.importer.kotlin;

import com.intellij.codeInsight.daemon.ReferenceImporter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.zhongan.devpilot.completions.autoimport.importer.DevpilotReferenceImporter;


import java.lang.reflect.Method;


public class DevpilotKotlinReferenceImporter extends DevpilotReferenceImporter {

    public DevpilotKotlinReferenceImporter(Editor editor, PsiFile file, int startOffset, int endOffset) {
        super(editor, file, startOffset, endOffset);
    }

    @Override
    public void computeReferences() {
        for (ReferenceImporter referenceImporter : ReferenceImporter.EP_NAME.getExtensionList()) {
            Class importerClass = referenceImporter.getClass();
            String importerClassName = importerClass.getName();
            if(importerClassName.equals("org.jetbrains.kotlin.idea.codeInsight.KotlinReferenceImporter")){
                Class<?>[] paramTypes = { Editor.class, PsiFile.class, Integer.TYPE, Boolean.TYPE };
                // 获取方法对象
                try {
                    Method importMethod = importerClass.getMethod("computeAutoImportAtOffset", paramTypes);
                    this.callComputeReferences(referenceImporter,importMethod);
                } catch (NoSuchMethodException e) {
                    this.manualImportReference();
                }
            }
        }
    }

    @Override
    protected void callComputeReferences(ReferenceImporter referenceImporter,Method importMethod) {
        Document document = myEditor.getDocument();
        int startLineNumber = document.getLineNumber(startOffset);
        int endLineNumber = document.getLineNumber(endOffset);
        for(int i = startLineNumber; i <= endLineNumber; i++) {
            int  lineStartOffset = document.getLineStartOffset(i);
                try {
                    importMethod.invoke(referenceImporter, myEditor, myFile, lineStartOffset, false);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    protected void manualImportReference() {
    }




}
