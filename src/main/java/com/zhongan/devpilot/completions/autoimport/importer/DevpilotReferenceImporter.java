package com.zhongan.devpilot.completions.autoimport.importer;

import com.intellij.codeInsight.daemon.ReferenceImporter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;

import java.lang.reflect.Method;

public class DevpilotReferenceImporter {

    protected Editor myEditor;
    protected PsiFile myFile;
    protected int startOffset;
    protected int endOffset;

    protected DevpilotReferenceImporter(Editor editor, PsiFile file, int startOffset, int endOffset){
        myEditor = editor;
        myFile = file;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public void computeReferences(){}

    protected void callComputeReferences(ReferenceImporter referenceImporter,Method method) {}

    protected void manualImportReference(){}
}
