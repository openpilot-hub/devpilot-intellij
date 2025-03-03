package com.zhongan.devpilot.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.provider.file.FileAnalyzeProviderFactory;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class PromptDataMapUtils {
    public static void buildCodePredictDataMap(Project project, List<CodeReferenceModel> codeReferences, Map<String, String> data) {
        var refs = new ArrayList<>();

        for (CodeReferenceModel codeReference : codeReferences) {
            var language = codeReference.getLanguageId();

            var ref = FileAnalyzeProviderFactory.getProvider(language)
                    .buildCodePredictDataMap(project, codeReference, data);

            if (ref != null) {
                refs.add(ref);
            }
        }

        if (!CollectionUtils.isEmpty(refs)) {
            var refsString = JsonUtils.toJson(refs);
            if (!StringUtils.isEmpty(refsString)) {
                data.put("refs", JsonUtils.toJson(refs));
            }
        }
    }

    public static void buildChatDataMap(Project project, PsiElement psiElement, List<CodeReferenceModel> codeReferences, Map<String, String> data) {
        List<Map<String, String>> refs = new ArrayList<>();

        for (CodeReferenceModel codeReference : codeReferences) {
            var ref = FileAnalyzeProviderFactory.getProvider(codeReference.getLanguageId())
                    .buildChatDataMap(project, psiElement, codeReference, data);

            if (ref != null) {
                refs.add(ref);
            }
        }

        if (!CollectionUtils.isEmpty(refs)) {
            var refsString = JsonUtils.toJson(refs);
            if (!StringUtils.isEmpty(refsString)) {
                data.put("refs", JsonUtils.toJson(refs));
            }
        }
    }
}
