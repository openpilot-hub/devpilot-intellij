package com.zhongan.devpilot.provider.file;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.embedding.entity.DevPilotFileInfo;
import com.zhongan.devpilot.embedding.entity.request.EmbeddingQueryResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCodePrediction;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import java.util.List;
import java.util.Map;

public interface FileAnalyzeProvider {
    default String languageName() {
        return "none";
    }

    default String moduleName() {
        return "default";
    }

    default void buildCodePredictDataMap(Project project, List<CodeReferenceModel> codeReferences, Map<String, String> data) {
    }

    default void buildChatDataMap(Project project, PsiElement psiElement, List<CodeReferenceModel> codeReferences, Map<String, String> data) {
    }

    default void buildTestDataMap(Project project, Editor editor, Map<String, String> data) {
    }

    default void buildRelatedContextDataMap(Project project, List<CodeReferenceModel> codeReferences, List<PsiElement> localRef, List<String> remoteRef, List<EmbeddingQueryResponse.HitData> localEmbeddingRef, Map<String, String> data) {
    }

    default List<PsiElement> callLocalRag(Project project, DevPilotCodePrediction codePrediction) {
        return List.of();
    }

    default DevPilotFileInfo parseFile(Project project, VirtualFile virtualFile) {
        return null;
    }
}
