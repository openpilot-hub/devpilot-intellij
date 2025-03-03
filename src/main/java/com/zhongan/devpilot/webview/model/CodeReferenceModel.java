package com.zhongan.devpilot.webview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.embedding.entity.request.EmbeddingQueryResponse;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.util.PsiElementUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static com.zhongan.devpilot.util.PsiElementUtils.shouldIgnorePsiElement;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeReferenceModel {
    private String languageId;

    private String fileUrl;

    private String fileName;

    private String sourceCode;

    private Integer selectedStartLine;

    private Integer selectedStartColumn;

    private Integer selectedEndLine;

    private Integer selectedEndColumn;

    private boolean visible = true;

    private String base64;

    @JsonIgnore
    private EditorActionEnum type;

    public CodeReferenceModel() {

    }

    public static EditorActionEnum getLastType(List<CodeReferenceModel> codeReferences) {
        if (CollectionUtils.isEmpty(codeReferences)) {
            return null;
        }

        var lastCode = codeReferences.get(codeReferences.size() - 1);
        return lastCode.getType();
    }

    public static String getLastSourceCode(List<CodeReferenceModel> codeReferences) {
        if (CollectionUtils.isEmpty(codeReferences)) {
            return null;
        }

        var lastCode = codeReferences.get(codeReferences.size() - 1);
        return lastCode.getSourceCode();
    }

    public static String getLanguage(List<CodeReferenceModel> codeReferences) {
        if (CollectionUtils.isEmpty(codeReferences)) {
            return DevPilotVersion.getDefaultLanguage();
        }

        // 目前多个代码片段只要有java代码就认为是java
        if (codeReferences.stream().anyMatch(codeReference -> "java".equals(codeReference.getLanguageId()))) {
            return "java";
        }

        var lastCode = codeReferences.get(codeReferences.size() - 1);
        var language = lastCode.getLanguageId();

        if (StringUtils.isEmpty(language)) {
            return DevPilotVersion.getDefaultLanguage();
        }

        return language;
    }

    public static CodeReferenceModel getCodeRefFromEditor(EditorInfo editorInfo, EditorActionEnum actionEnum) {
        return new CodeReferenceModel(editorInfo.getLanguageId(), editorInfo.getFilePresentableUrl(),
                editorInfo.getFileName(), editorInfo.getSourceCode(), editorInfo.getSelectedStartLine(),
                editorInfo.getSelectedStartColumn(), editorInfo.getSelectedEndLine(), editorInfo.getSelectedEndColumn(), actionEnum);
    }

    public static List<CodeReferenceModel> getCodeRefFromRag(
            Project project, Collection<EmbeddingQueryResponse.HitData> codeList, String languageId) {
        if (codeList == null) {
            return null;
        }

        var result = new ArrayList<CodeReferenceModel>();

        for (var data : codeList) {
            var code = PsiElementUtils.getCodeBlock(project, data.getFilePath(), data.getStartOffset(), data.getEndOffset());
            if (code == null) {
                continue;
            }
            var absolutePath = project.getBasePath() + File.separator + data.getFilePath();
            var fileName = data.getFilePath().substring(data.getFilePath().lastIndexOf(File.separator) + 1);
            var ref = new CodeReferenceModel(languageId, absolutePath, fileName, code,
                    data.getStartLine(), data.getStartColumn(), data.getEndLine(), data.getEndColumn(), null);
            result.add(ref);
        }

        return result;
    }

    public static List<CodeReferenceModel> getCodeRefFromString(Collection<String> codeList, String languageId) {
        if (codeList == null) {
            return null;
        }

        var result = new ArrayList<CodeReferenceModel>();

        for (String code : codeList) {
            var ref = new CodeReferenceModel(languageId, null,
                    null, code, null, null, null, null, null);
            result.add(ref);
        }

        return result;
    }

    public static List<CodeReferenceModel> getCodeRefListFromPsiElement(Collection<PsiElement> list, EditorActionEnum actionEnum) {
        if (list == null) {
            return null;
        }

        var result = new ArrayList<CodeReferenceModel>();

        for (PsiElement element : list) {
            if (shouldIgnorePsiElement(element)) {
                continue;
            }
            var ref = getCodeRefFromPsiElement(element, actionEnum);
            if (ref != null) {
                result.add(ref);
            }
        }

        return result;
    }

    public static CodeReferenceModel getCodeRefFromPsiElement(PsiElement element, EditorActionEnum actionEnum) {
        if (element == null) {
            return null;
        }

        var languageId = element.getLanguage().getID();
        var sourceCode = element.getText();

        var psiFile = element.getContainingFile();
        VirtualFile file = null;
        Document document = null;

        if (psiFile != null) {
            file = psiFile.getVirtualFile();
            var project = element.getProject();
            document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        }

        String filePath = null;
        String fileName = null;

        if (file != null) {
            filePath = file.getPath();
            fileName = file.getName();
        }

        Integer startLine = null;
        Integer endLine = null;

        Integer startColumn = null;
        Integer endColumn = null;

        if (document != null) {
            var textRange = element.getTextRange();
            int startOffset = textRange.getStartOffset();
            int endOffset = textRange.getEndOffset();

            startLine = document.getLineNumber(startOffset);
            endLine = document.getLineNumber(endOffset);

            startColumn = startOffset - document.getLineStartOffset(startLine);
            endColumn = endOffset - document.getLineStartOffset(endLine);
        }

        return new CodeReferenceModel(
                languageId, filePath, fileName, sourceCode, startLine, startColumn, endLine, endColumn, actionEnum);
    }

    public CodeReferenceModel(String languageId, String fileUrl, String fileName, String sourceCode,
                              Integer selectedStartLine, Integer selectedStartColumn,
                              Integer selectedEndLine, Integer selectedEndColumn, EditorActionEnum type) {
        this.languageId = languageId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.sourceCode = sourceCode;
        this.selectedStartLine = selectedStartLine;
        this.selectedStartColumn = selectedStartColumn;
        this.selectedEndLine = selectedEndLine;
        this.selectedEndColumn = selectedEndColumn;
        this.type = type;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Integer getSelectedStartLine() {
        return selectedStartLine;
    }

    public void setSelectedStartLine(Integer selectedStartLine) {
        this.selectedStartLine = selectedStartLine;
    }

    public Integer getSelectedStartColumn() {
        return selectedStartColumn;
    }

    public void setSelectedStartColumn(Integer selectedStartColumn) {
        this.selectedStartColumn = selectedStartColumn;
    }

    public Integer getSelectedEndLine() {
        return selectedEndLine;
    }

    public void setSelectedEndLine(Integer selectedEndLine) {
        this.selectedEndLine = selectedEndLine;
    }

    public Integer getSelectedEndColumn() {
        return selectedEndColumn;
    }

    public void setSelectedEndColumn(Integer selectedEndColumn) {
        this.selectedEndColumn = selectedEndColumn;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public EditorActionEnum getType() {
        return type;
    }

    public void setType(EditorActionEnum type) {
        this.type = type;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }
}
