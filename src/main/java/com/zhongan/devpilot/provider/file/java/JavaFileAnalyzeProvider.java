package com.zhongan.devpilot.provider.file.java;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.zhongan.devpilot.constant.PromptConst;
import com.zhongan.devpilot.embedding.entity.DevPilotFileInfo;
import com.zhongan.devpilot.embedding.entity.FileMeta;
import com.zhongan.devpilot.embedding.entity.FunctionMeta;
import com.zhongan.devpilot.embedding.entity.FunctionPartBlockMeta;
import com.zhongan.devpilot.embedding.entity.java.anno.AnnotationAttributeMeta;
import com.zhongan.devpilot.embedding.entity.java.anno.AnnotationMeta;
import com.zhongan.devpilot.embedding.entity.java.file.JavaFileMeta;
import com.zhongan.devpilot.embedding.entity.java.function.JavaFunctionMeta;
import com.zhongan.devpilot.embedding.entity.request.EmbeddingQueryResponse;
import com.zhongan.devpilot.enums.UtFrameTypeEnum;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCodePrediction;
import com.zhongan.devpilot.provider.file.FileAnalyzeProvider;
import com.zhongan.devpilot.provider.ut.UtFrameworkProvider;
import com.zhongan.devpilot.provider.ut.UtFrameworkProviderFactory;
import com.zhongan.devpilot.util.MD5Utils;
import com.zhongan.devpilot.util.PsiElementUtils;
import com.zhongan.devpilot.util.PsiFileUtil;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
    public Map<String, String> buildCodePredictDataMap(Project project, CodeReferenceModel codeReference, Map<String, String> data) {
        Map<String, String> ref = null;

        var psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, codeReference.getFileUrl());
        if (psiJavaFile != null) {
            ref = Map.of(
                    "imports", PsiElementUtils.getImportInfo(psiJavaFile),
                    "package", psiJavaFile.getPackageName(),
                    "fields", PsiElementUtils.getFieldList(psiJavaFile),
                    "selectedCode", codeReference.getSourceCode(),
                    "filePath", codeReference.getFileUrl()
            );
        }

        return ref;
    }

    @Override
    public Map<String, String> buildChatDataMap(Project project, PsiElement psiElement, CodeReferenceModel codeReference, Map<String, String> data) {
        var psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, codeReference.getFileUrl());

        Map<String, String> map = new HashMap<>();

        if (psiJavaFile != null) {
            map.putAll(Map.of(
                    "imports", PsiElementUtils.getImportInfo(psiJavaFile),
                    "package", psiJavaFile.getPackageName(),
                    "fields", PsiElementUtils.getFieldList(psiJavaFile),
                    "filePath", codeReference.getFileUrl(),
                    "selectedCode", codeReference.getSourceCode(),
                    "language", "java"
            ));
        }

        if (psiElement != null) {
            var fullClassName = PsiElementUtils.getFullClassName(psiElement);

            if (fullClassName != null) {
                map.put(CLASS_FULL_NAME, fullClassName);
            }
        }

        if (map.isEmpty()) {
            return null;
        }

        return map;
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
    public void buildRelatedContextDataMap(Project project, List<CodeReferenceModel> codeReferences,
                                           List<PsiElement> localRef, List<String> remoteRef,
                                           List<EmbeddingQueryResponse.HitData> localEmbeddingRef, Map<String, String> data) {
        String packageName = null;

        if (CollectionUtils.isNotEmpty(codeReferences)) {
            var codeReference = codeReferences.get(codeReferences.size() - 1);

            if (codeReference.getFileUrl() != null) {
                var psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, codeReference.getFileUrl());
                if (psiJavaFile != null) {
                    packageName = psiJavaFile.getPackageName();
                }
            }
        }

        String relatedCode = "";

        if (localRef != null && !localRef.isEmpty()) {
            relatedCode = PsiElementUtils.transformElementToString(localRef, packageName);
        }

        if (localEmbeddingRef != null && !localEmbeddingRef.isEmpty()) {
            String codeList = localEmbeddingRef.stream()
                    .map(hitData -> PsiElementUtils.getCodeBlock(project,
                            hitData.getFilePath(), hitData.getStartOffset(), hitData.getEndOffset()))
                    .filter(code -> !StringUtils.isEmpty(code))
                    .collect(Collectors.joining("\n"));

            if (!StringUtils.isEmpty(codeList)) {
                relatedCode += codeList;
            }
        }

        if (!relatedCode.isEmpty()) {
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

    @Override
    public DevPilotFileInfo parseFile(Project project, VirtualFile virtualFile) {
        PsiJavaFile psiJavaFile = PsiElementUtils.getPsiJavaFileByFilePath(project, virtualFile.getPath());
        if (null == psiJavaFile) {
            return null;
        }

        // 确认内部类情况
        PsiClass psiClass = PsiElementUtils.getPsiClassByFile(psiJavaFile);
        if (null == psiClass) {
            return null;
        }

        DevPilotFileInfo fileInfo = new DevPilotFileInfo();
        fileInfo.setFilePath(StringUtils.replace(virtualFile.getPath(), project.getBasePath() + File.separator, ""));
        fileInfo.setFileMeta(buildFileMeta(project, psiJavaFile, psiClass));
        fileInfo.setFileName(psiJavaFile.getName());
        fileInfo.setFileHash(MD5Utils.calculateMD5(virtualFile));
        return fileInfo;
    }

    private FileMeta buildFileMeta(Project project, PsiJavaFile psiJavaFile, PsiClass psiClass) {
        JavaFileMeta fileMeta = new JavaFileMeta();
        fileMeta.setFileType("java");

        var startOffset = psiJavaFile.getTextRange().getStartOffset();
        var endOffset = psiJavaFile.getTextRange().getEndOffset();

        fileMeta.setStartOffset(startOffset);
        fileMeta.setEndOffset(endOffset);

        var document = PsiDocumentManager.getInstance(project).getDocument(psiJavaFile);
        if (document != null) {
            var startLine = document.getLineNumber(startOffset);
            var endLine = document.getLineNumber(endOffset);

            var startColumn = startOffset - document.getLineStartOffset(startLine);
            var endColumn = endOffset - document.getLineStartOffset(endLine);

            fileMeta.setStartLine(startLine);
            fileMeta.setEndLine(endLine);
            fileMeta.setStartColumn(startColumn);
            fileMeta.setEndColumn(endColumn);
        }

        fileMeta.setPackageName(psiJavaFile.getPackageName());
        fileMeta.setImports(PsiElementUtils.getImportInfo(psiJavaFile));
        fileMeta.setClassDeclaration(generateClassDeclaration(psiClass));
        fileMeta.setTypeName(psiClass.getName());
        // 内部类处理方式待调整
        fileMeta.setTypeFullName(psiJavaFile.getPackageName() + "." + psiJavaFile.getName());
        List<FunctionMeta> functionMetas = buildFunctionMeta(project, psiJavaFile, psiClass);
        fileMeta.setFunctionMetas(functionMetas);

        PsiField[] fields = psiClass.getAllFields();
        fileMeta.setFields(Arrays.stream(fields)
                .map(field -> getByPsiType(field.getType()))
                .collect(Collectors.toList()));

        fileMeta.setAnnotationMetas(convertPsiAnnotations(psiClass.getAnnotations()));

        PsiDocComment classDocComment = psiClass.getDocComment();
        if (null != classDocComment) {
            fileMeta.setComments(classDocComment.getText());
        }
        fileMeta.setLlmSummary("TODO::");

        fileMeta.setClazzDef(getClazzDefWithoutImports(psiJavaFile, psiClass));
        return fileMeta;
    }

    private String generateClassDeclaration(PsiClass psiClass) {
        // 构建类声明信息
        StringBuilder classDeclaration = new StringBuilder();

        // 添加类的修饰符
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null) {
            classDeclaration.append(modifierList.getText()).append(" ");
        }

        // 添加类的类型（class, interface, enum等）
        classDeclaration.append(psiClass.isInterface() ? "interface " :
                psiClass.isEnum() ? "enum " : "class ");

        // 添加类名
        classDeclaration.append(psiClass.getName());

        // 添加泛型参数（如果有）
        PsiTypeParameterList typeParameterList = psiClass.getTypeParameterList();
        if (typeParameterList != null && typeParameterList.getTypeParameters().length > 0) {
            classDeclaration.append(typeParameterList.getText());
        }

        // 添加继承的类（如果有）
        PsiReferenceList extendsList = psiClass.getExtendsList();
        if (extendsList != null && extendsList.getReferenceElements().length > 0) {
            classDeclaration.append(" extends ").append(extendsList.getText());
        }

        // 添加实现的接口（如果有）
        PsiReferenceList implementsList = psiClass.getImplementsList();
        if (implementsList != null && implementsList.getReferenceElements().length > 0) {
            classDeclaration.append(" implements ").append(implementsList.getText());
        }
        return classDeclaration.toString();
    }

    private List<FunctionMeta> buildFunctionMeta(Project project, PsiJavaFile psiJavaFile, PsiClass psiClass) {
        List<FunctionMeta> functionMetas = new ArrayList<>();
        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            if (PropertyUtil.isSimpleGetter(method) || PropertyUtil.isSimpleSetter(method)) {
                continue;
            }
            JavaFunctionMeta functionMeta = new JavaFunctionMeta();

            functionMeta.setFunctionLLMSummary("TODO::");
            functionMeta.setAnnotations(convertPsiAnnotations(method.getAnnotations()));
            functionMeta.setName(method.getName());

            functionMeta.setSignature(PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY,
                    PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_TYPE | PsiFormatUtilBase.SHOW_PARAMETERS,
                    PsiFormatUtilBase.SHOW_TYPE));

            if (method.getTextRange() == null) {
                continue;
            }

            var startOffset = method.getTextRange().getStartOffset();
            var endOffset = method.getTextRange().getEndOffset();
            if (startOffset == 0 && endOffset == 0) {
                continue;
            }

            var document = PsiDocumentManager.getInstance(project).getDocument(psiJavaFile);
            if (document == null) {
                continue;
            }

            var startLine = document.getLineNumber(startOffset);
            var endLine = document.getLineNumber(endOffset);

            var startColumn = startOffset - document.getLineStartOffset(startLine);
            var endColumn = endOffset - document.getLineStartOffset(endLine);

            functionMeta.setFunctionStartOffset(startOffset);
            functionMeta.setFunctionEndOffset(endOffset);
            functionMeta.setStartLine(startLine);
            functionMeta.setEndLine(endLine);
            functionMeta.setStartColumn(startColumn);
            functionMeta.setEndColumn(endColumn);
            functionMeta.setReturnType(getReturnType(method));

            PsiDocComment docComment = method.getDocComment();
            int codeStartOffset = functionMeta.getFunctionStartOffset();

            if (null != docComment) {
                functionMeta.setComments(docComment.getText());
                codeStartOffset = docComment.getTextRange().getEndOffset();
            }
            functionMeta.setContent(method.getText().substring(codeStartOffset - functionMeta.getFunctionStartOffset()));

            PsiParameter[] parameters = method.getParameterList().getParameters();
            functionMeta.setArguments(Arrays.stream(parameters)
                    .filter(i -> i.getType() instanceof PsiClassReferenceType)
                    .map(parameter -> getByPsiType(parameter.getType()))
                    .collect(Collectors.toList()));

            functionMeta.setCodeBlocks(buildFunctionPartBlockMeta(method, functionMeta, codeStartOffset));
            functionMetas.add(functionMeta);
        }
        return functionMetas;
    }

    private List<FunctionPartBlockMeta> buildFunctionPartBlockMeta(PsiMethod method, JavaFunctionMeta functionMeta, int codeStartOffset) {
        List<FunctionPartBlockMeta> codeBlockMetas = new ArrayList<>();

        String methodText = method.getText().substring(codeStartOffset - functionMeta.getFunctionStartOffset());
        for (int i = 0; i < methodText.length(); i += 20) {
            FunctionPartBlockMeta blockMeta = new FunctionPartBlockMeta();
            int endIndex = Math.min(i + 20, methodText.length());
            String chunk = methodText.substring(i, endIndex);

            blockMeta.setBlockStartOffset(codeStartOffset + i);
            blockMeta.setBlockEndOffset(codeStartOffset + endIndex);
            blockMeta.setCodeBody(chunk);

            codeBlockMetas.add(blockMeta);
        }
        return codeBlockMetas;
    }

    private List<AnnotationMeta> convertPsiAnnotations(PsiAnnotation[] annotations) {
        List<AnnotationMeta> annotationMetas = new ArrayList<>();
        for (PsiAnnotation annotation : annotations) {
            annotationMetas.add(buildAnnotationMeta(annotation));
        }
        return annotationMetas;
    }

    private AnnotationMeta buildAnnotationMeta(PsiAnnotation annotation) {
        AnnotationMeta annotationMeta = new AnnotationMeta();
        annotationMeta.setType(annotation.getQualifiedName());

        PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
            String name = Optional.ofNullable(attribute.getName()).orElse("value");
            PsiAnnotationMemberValue value = attribute.getValue();
            AnnotationAttributeMeta annotationAttributeMeta = new AnnotationAttributeMeta();
            annotationAttributeMeta.setName(name);
            annotationAttributeMeta.setValue(getAttributeValue(value));
            annotationMeta.getAttributes().add(annotationAttributeMeta);
        }
        return annotationMeta;
    }

    private String getAttributeValue(PsiAnnotationMemberValue value) {
        if (value instanceof PsiLiteralExpression) {
            return Optional.ofNullable(((PsiLiteralExpression) value).getValue()).orElse(StringUtils.EMPTY).toString();
        } else if (value instanceof PsiReferenceExpression) {
            return ((PsiReferenceExpression) value).getReferenceName();
        } else if (value instanceof PsiArrayInitializerMemberValue) {
            PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue) value).getInitializers();
            return Arrays.stream(initializers)
                    .map(this::getAttributeValue)
                    .collect(Collectors.joining(", ", "{", "}"));
        } else if (value instanceof PsiExpression) {
            return value.getText();
        }
        return value.getText();
    }

    private static DevPilotFileInfo getReturnType(PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (null == returnType) {
            return null;
        }
        return getByPsiType(returnType);
    }

    private static DevPilotFileInfo getByPsiType(PsiType psiType) {
        DevPilotFileInfo fileInfo = new DevPilotFileInfo();
        JavaFileMeta fileMeta = new JavaFileMeta();
        fileMeta.setFileType("java");

        if (psiType instanceof PsiClassReferenceType) {
            fileMeta.setTypeName(((PsiClassReferenceType) psiType).getClassName());
            fileMeta.setTypeFullName(psiType.getCanonicalText());
        } else {
            fileMeta.setTypeName(psiType.getCanonicalText());
            fileMeta.setTypeFullName(psiType.getCanonicalText());
        }
        fileInfo.setFileMeta(fileMeta);
        return fileInfo;
    }

    public static String getClazzDef(PsiJavaFile psiJavaFile, PsiClass psiClass) {
        String result = StringUtils.EMPTY;
        PsiElement lBrace = psiClass.getLBrace();
        if (lBrace != null) {
            int startOffset = lBrace.getTextRange().getEndOffset();
            result = psiJavaFile.getText().substring(0, startOffset);
            result += "\n    // ...\n}";
        }
        return result;
    }

    public static String getClazzDefWithoutImports(PsiJavaFile psiJavaFile, PsiClass psiClass) {
        String fullText = getClazzDef(psiJavaFile, psiClass);

        String withoutImports = fullText.replaceAll("(?m)^import\\s+.*?;\n", "");
        withoutImports = withoutImports.replaceAll("(?m)^\\s*\n", "");

        return withoutImports;
    }
}
