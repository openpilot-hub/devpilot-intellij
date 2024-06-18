package com.zhongan.devpilot.completions.autoimport.importer.java;

import com.intellij.codeInsight.daemon.ReferenceImporter;
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil;
import com.intellij.codeInsight.daemon.impl.JavaReferenceImporter;
import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFix;
import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.*;
import com.zhongan.devpilot.completions.autoimport.importer.DevpilotReferenceImporter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;


public class DevpilotJavaReferenceImporter extends DevpilotReferenceImporter {
    public DevpilotJavaReferenceImporter(Editor editor, PsiFile file, int startOffset, int endOffset) {
        super(editor, file, startOffset, endOffset);
    }

    @Override
    public void computeReferences() {
        this.manualImportReference();
    }

    protected void manualImportReference() {
        Document document = myEditor.getDocument();
        int importLineNumber = document.getLineNumber(startOffset);
        while(importLineNumber < document.getLineCount()){
            List<ImportClassFix> fixes = computeImportFix(myFile, document.getLineStartOffset(importLineNumber), document.getLineEndOffset(importLineNumber));
            if(fixes != null &&!fixes.isEmpty() ) {
                boolean alreadyImported = false;
                for(ImportClassFix fix: fixes) {
                    ImportClassFixBase.Result result = loopManualImportReference(fix);
                    if(result == ImportClassFixBase.Result.CLASS_AUTO_IMPORTED) {
                        alreadyImported = true;
                        break;
                    }
                }
                if(!alreadyImported)importLineNumber++;
            }
            else{
                importLineNumber++;
            }
        }
    }


    protected ImportClassFixBase.Result loopManualImportReference(ImportClassFix fix) {
        FutureTask<ImportClassFixBase.Result> writetask = new FutureTask(() -> {
            try {
                return fix.doFix(myEditor, false, true, true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(writetask);
        });
        try {
            return writetask.get();
        } catch (InterruptedException e) {
            return ImportClassFixBase.Result.POPUP_NOT_SHOWN;
        } catch (ExecutionException e) {
            return ImportClassFixBase.Result.POPUP_NOT_SHOWN;
        }
    }

    private List<ImportClassFix> computeImportFix(PsiFile file,int startOffset,int endOffset){
        FutureTask<List<PsiElement>> readElementTask = new FutureTask<>(() ->CollectHighlightsUtil.getElementsInRange(file, startOffset, endOffset) );
        ApplicationManager.getApplication().runReadAction(readElementTask);
        List<PsiElement> elements = null;
        try {
            elements = readElementTask.get();
        } catch (Exception e){
            return null;
        }
        List<PsiElement> finalElements = elements;
        FutureTask<List<ImportClassFix>> readFixTask = new FutureTask<>(() -> {
            List<ImportClassFix> importFixes = new ArrayList<>();
            for (PsiElement element : finalElements) {
                if (element instanceof PsiJavaCodeReferenceElement) {
                    PsiJavaCodeReferenceElement ref = (PsiJavaCodeReferenceElement)element;
                    ImportClassFix fix = new ImportClassFix(ref);
                    if (fix.isAvailable(file.getProject(), null, file)) {
                        importFixes.add(fix);
                    }
                }
            }
            return importFixes;
        });
        ApplicationManager.getApplication().runReadAction(readFixTask);
        try {
            return readFixTask.get();
        } catch (Exception e){
            return null;
        }
    }

    /**
     * 这个函数是触发idea的自动导包方法，目前的触发方式触发不了
     * @param referenceImporter
     * @param importMethod
     */
    @Override
    protected void callComputeReferences(ReferenceImporter referenceImporter,Method importMethod) {
        Document document = myEditor.getDocument();
        int startLineNumber = document.getLineNumber(startOffset);
        int endLineNumber = document.getLineNumber(endOffset);
        for (int i = startLineNumber; i <= endLineNumber; i++) {
            int lineStartOffset = document.getLineStartOffset(i);

            FutureTask<Boolean> writeTask = new FutureTask<>(() ->{
                try {
                    importMethod.invoke(referenceImporter, myEditor, myFile, lineStartOffset, false);
                    return true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });

            final Runnable command = () -> ApplicationManager.getApplication().runWriteAction(writeTask);
            ProgressManager.getInstance().executeProcessUnderProgress(() -> {
                ApplicationManagerEx.getApplicationEx().tryRunReadAction(command);
            },ProgressIndicatorProvider.getGlobalProgressIndicator());

        }
    }
}
