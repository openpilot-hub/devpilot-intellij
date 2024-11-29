package com.zhongan.devpilot.provider.file.java;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.constant.PromptConst;
import com.zhongan.devpilot.enums.UtFrameTypeEnum;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCodePrediction;
import com.zhongan.devpilot.provider.file.FileAnalyzeProvider;
import com.zhongan.devpilot.provider.ut.UtFrameworkProvider;
import com.zhongan.devpilot.provider.ut.UtFrameworkProviderFactory;
import com.zhongan.devpilot.util.PsiElementUtils;
import com.zhongan.devpilot.util.PsiFileUtil;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import java.util.List;
import java.util.Map;

import static com.zhongan.devpilot.constant.PlaceholderConst.ADDITIONAL_MOCK_PROMPT;
import static com.zhongan.devpilot.constant.PlaceholderConst.CLASS_FULL_NAME;
import static com.zhongan.devpilot.constant.PlaceholderConst.MOCK_FRAMEWORK;
import static com.zhongan.devpilot.constant.PlaceholderConst.TEST_FRAMEWORK;

public class JavaFileAnalyzeProvider implements FileAnalyzeProvider {
    @Override
    public String languageName() {
        return "java";
    }

    @Override
    public String moduleName() {
        return "com.intellij.java";
    }

    @Override
    public void buildCodePredictDataMap(Project project, CodeReferenceModel codeReference, Map<String, String> data) {
        var psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, codeReference.getFileUrl());

        if (psiJavaFile != null) {
            data.putAll(
                    Map.of(
                            "imports", PsiElementUtils.getImportInfo(psiJavaFile),
                            "package", psiJavaFile.getPackageName(),
                            "fields", PsiElementUtils.getFieldList(psiJavaFile),
                            "selectedCode", codeReference.getSourceCode(),
                            "filePath", codeReference.getFileUrl()
                    )
            );
        }
    }

    @Override
    public void buildChatDataMap(Project project, PsiElement psiElement, CodeReferenceModel codeReference, Map<String, String> data) {
        var psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, codeReference.getFileUrl());

        if (psiJavaFile != null) {
            data.putAll(
                    Map.of(
                            "imports", PsiElementUtils.getImportInfo(psiJavaFile),
                            "package", psiJavaFile.getPackageName(),
                            "fields", PsiElementUtils.getFieldList(psiJavaFile),
                            "filePath", codeReference.getFileUrl(),
                            "language", "java"
                    )
            );
        }

        if (psiElement != null) {
            var fullClassName = PsiElementUtils.getFullClassName(psiElement);

            if (fullClassName != null) {
                data.put(CLASS_FULL_NAME, fullClassName);
            }
        }
    }

    @Override
    public void buildTestDataMap(Project project, Editor editor, Map<String, String> data) {
        if (PsiFileUtil.isCaretInWebClass(project, editor)) {
            data.put(ADDITIONAL_MOCK_PROMPT, PromptConst.MOCK_WEB_MVC);
        }
        UtFrameworkProvider utFrameworkProvider = UtFrameworkProviderFactory.create(languageName());
        if (utFrameworkProvider != null) {
            UtFrameTypeEnum utFramework = utFrameworkProvider.getUTFramework(project, editor);
            data.put(TEST_FRAMEWORK, utFramework.getUtFrameType());
            data.put(MOCK_FRAMEWORK, utFramework.getMockFrameType());
        }
    }

    @Override
    public void buildRelatedContextDataMap(Project project, CodeReferenceModel codeReference,
                                           List<PsiElement> localRef, List<String> remoteRef, Map<String, String> data) {
        String packageName = null;

        if (codeReference != null && codeReference.getFileUrl() != null) {
            var psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, codeReference.getFileUrl());
            if (psiJavaFile != null) {
                packageName = psiJavaFile.getPackageName();
            }
        }

        if (localRef != null && !localRef.isEmpty()) {
            var relatedCode = PsiElementUtils.transformElementToString(localRef, packageName);
            data.put("relatedContext", relatedCode);
        }

        if (remoteRef != null && !remoteRef.isEmpty()) {
            data.put("additionalRelatedContext", String.join("\n", remoteRef));
        }
    }

    @Override
    public List<PsiElement> callLocalRag(Project project, DevPilotCodePrediction codePrediction) {
        return PsiElementUtils.contextRecall(project, codePrediction);
    }
}
