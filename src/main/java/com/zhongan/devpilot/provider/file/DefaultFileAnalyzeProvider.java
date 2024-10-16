package com.zhongan.devpilot.provider.file;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCodePrediction;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import java.util.List;
import java.util.Map;

public class DefaultFileAnalyzeProvider implements FileAnalyzeProvider {
    @Override
    public String languageName() {
        return "none";
    }

    @Override
    public String moduleName() {
        return "default";
    }

    @Override
    public void buildCodePredictDataMap(Project project, CodeReferenceModel codeReference, Map<String, String> data) {
        // default do nothing
    }

    @Override
    public void buildChatDataMap(Project project, PsiElement psiElement, CodeReferenceModel codeReference, Map<String, String> data) {
        // default do nothing
    }

    @Override
    public void buildTestDataMap(Project project, Editor editor, Map<String, String> data) {
        // default do nothing
    }

    @Override
    public void buildRelatedContextDataMap(Project project, CodeReferenceModel codeReference, List<PsiElement> localRef, List<PsiElement> remoteRef, Map<String, String> data) {

    }

    @Override
    public List<PsiElement> callLocalRag(Project project, DevPilotCodePrediction codePrediction) {
        return List.of();
    }
}
