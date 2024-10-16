package com.zhongan.devpilot.provider.file;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCodePrediction;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import java.util.List;
import java.util.Map;

public interface FileAnalyzeProvider {
    String languageName();

    String moduleName();

    void buildCodePredictDataMap(Project project, CodeReferenceModel codeReference, Map<String, String> data);

    void buildChatDataMap(Project project, PsiElement psiElement, CodeReferenceModel codeReference, Map<String, String> data);

    void buildTestDataMap(Project project, Editor editor, Map<String, String> data);

    void buildRelatedContextDataMap(Project project, CodeReferenceModel codeReference, List<PsiElement> localRef, List<PsiElement> remoteRef, Map<String, String> data);

    List<PsiElement> callLocalRag(Project project, DevPilotCodePrediction codePrediction);
}
