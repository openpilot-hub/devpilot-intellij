package com.zhongan.codeai.util;

import com.google.common.collect.Lists;
import com.intellij.diff.DiffContentFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DocumentUtil
 *
 * @author zhangzhisheng
 * @version v0.1 2023-10-14 16:58 zhangzhisheng Exp $
 */
public class DocumentUtil {

    public static final List<String> CODE_FLAGS = Lists.newArrayList("(?s)```[\\s\\S]*?```", "\\b(public|class|def|function|var|let)\\b");

    public static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]");

    public static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");

    private static final DiffContentFactory diffContentFactory = DiffContentFactory.getInstance();

    /**
     * insert comment and format code
     *
     * @param project
     * @param editor
     * @param result
     */
    public static void insertCommentAndFormat(Project project, Editor editor, String result) {
        //insert comment, do not check result have code block
        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            int caretOffset = editor.getSelectionModel().getSelectionStart();
            document.replaceString(editor.getSelectionModel().getSelectionStart(),
                    editor.getSelectionModel().getSelectionEnd(),
                    result);
            // format code
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            codeStyleManager.reformatText(PsiManager.getInstance(project).findFile(file),
                    caretOffset, caretOffset + result.length());
        }));
    }

    /**
     * diff comment and format
     *
     * @param project
     * @param editor
     * @param result
     */
    public static void diffCommentAndFormatWindow(Project project, Editor editor, String result) {
        var selectionModel = editor.getSelectionModel();
        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
            VirtualFile createdFile = VirtualFileUtil.createVirtualReplaceFile(editor, project);
            Document replaceDocument = FileDocumentManager.getInstance().getDocument(createdFile);

            replaceDocument.setText(editor.getDocument().getText());
            replaceDocument.setReadOnly(false);
            replaceDocument.replaceString(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), result);

            //auto code format
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            codeStyleManager.reformatText(PsiDocumentManager.getInstance(project).getPsiFile(replaceDocument),
                    selectionModel.getSelectionStart(), selectionModel.getSelectionStart() + result.length());
            //show diff
            PerformanceCheckUtils.showDiff(project, editor, FileDocumentManager.getInstance().getFile(editor.getDocument()), replaceDocument);
        }));
    }

    public static boolean containsCode(String content) {
        for (String regex : CODE_FLAGS) {
            if (Pattern.compile(regex).matcher(content).find()) {
                return true;
            }
        }
        return false;
    }

    public static int getChineseCharCount(String text) {
        Matcher chineseMatcher = CHINESE_PATTERN.matcher(text);
        int chineseCount = 0;
        while (chineseMatcher.find()) {
            chineseCount++;
        }
        return chineseCount;
    }

    public static int getEnglishCharCount(String text) {
        Matcher englishMatcher = ENGLISH_PATTERN.matcher(text);
        int englishCount = 0;
        while (englishMatcher.find()) {
            englishCount++;
        }
        return englishCount;
    }

}