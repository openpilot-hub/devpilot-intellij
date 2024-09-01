package com.zhongan.devpilot.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class NewFileUtils {
    public static void createNewFile(Project project, String generatedText, CodeReferenceModel codeReferenceModel, String lang) {
        // if lang is null, set default language to java
        if (StringUtils.isEmpty(lang)) {
            handleDefaultAction(project, generatedText, ".java");
        }

        String fileExtension = MarkdownUtil.getFileExtensionFromLanguage(lang);

        if (codeReferenceModel != null && ".java".equals(fileExtension)
                && codeReferenceModel.getType() == EditorActionEnum.GENERATE_TESTS) {
            var fileUrl = codeReferenceModel.getFileUrl();
            var fileName = codeReferenceModel.getFileName();
            // java test will goto special logic
            handleGenerateTestsAction(project, generatedText, fileExtension, fileName, fileUrl);
        } else {
            handleDefaultAction(project, generatedText, fileExtension);
        }
    }

    private static void handleGenerateTestsAction(Project project, String generatedText, String fileExtension, String filename, String fileUrl) {
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(fileUrl);

        if (vf == null) {
            return;
        }

        String testFileDirPath = vf.getParent().getPath().replace("main", "test");
        PsiDirectory testPsiDir = createPsiDirectory(project, testFileDirPath);

        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(fileExtension);
        String fileNameWithoutExtension = filename.substring(0, filename.lastIndexOf("."));
        String testFileName = fileNameWithoutExtension + "Test" + fileExtension;

        if (testPsiDir.findFile(testFileName) != null) {
            BalloonAlertUtils.showErrorAlert(DevPilotMessageBundle.get("devpilot.alter.file.exist"), 0, -10, Balloon.Position.above);
            FileEditorManager.getInstance(project).openFile(testPsiDir.findFile(testFileName).getVirtualFile(), true);
            return;
        }

        generatedText = modifyGeneratedTextClassName(generatedText, testFileName);
        String finalGeneratedText = generatedText;
        DumbService.getInstance(project).runWhenSmart(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(testFileName, fileType, finalGeneratedText);
                PsiFile createdFile = (PsiFile) testPsiDir.add(fileFromText);
                FileEditorManager.getInstance(project).openFile(createdFile.getVirtualFile(), true);
            });
        });
    }

    private static void handleDefaultAction(Project project, String generatedText, String fileExtension) {
        PsiDirectory selectedFilePsiDir = getCurrentSelectedFilePsiDir(project);
        if (selectedFilePsiDir == null) {
            selectedFilePsiDir = createPsiDirectory(project, project.getBasePath());
        }

        String fileName = "temp_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        if (".java".equals(fileExtension)) {
            String generatedClassName = extraClassNameFromGeneratedText(generatedText);
            if (generatedClassName != null) {
                fileName = generatedClassName + fileExtension;
            }
        }

        if (selectedFilePsiDir.findFile(fileName) != null) {
            BalloonAlertUtils.showErrorAlert(DevPilotMessageBundle.get("devpilot.alter.file.exist"), 0, -10, Balloon.Position.above);
            FileEditorManager.getInstance(project).openFile(selectedFilePsiDir.findFile(fileName).getVirtualFile(), true);
            return;
        }

        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(fileExtension.substring(1));
        PsiDirectory finalSelectedFileDir = selectedFilePsiDir;
        String finalFileName = fileName;
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(finalFileName, fileType, generatedText);
            PsiFile createdFile = (PsiFile) finalSelectedFileDir.add(fileFromText);
            FileEditorManager.getInstance(project).openFile(createdFile.getVirtualFile(), true);
        });
    }

    private static PsiDirectory createPsiDirectory(Project project, String dirPath) {
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            DevPilotNotification.error(DevPilotMessageBundle.get("devpilot.notification.create.dir.failed"));
        }
        VirtualFile dirVF = LocalFileSystem.getInstance().refreshAndFindFileByPath(dirPath);
        return dirVF == null ? null : PsiDirectoryFactory.getInstance(project).createDirectory(dirVF);
    }

    private static String extraClassNameFromGeneratedText(String generatedText) {
        String regex = "(?:public\\s+)?(?:abstract\\s+|final\\s+)?(class|interface|enum)\\s+(\\w+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(generatedText);

        String generatedClassName = null;
        if (matcher.find()) {
            generatedClassName = matcher.group(2);
        }
        return generatedClassName;
    }

    private static String modifyGeneratedTextClassName(String generatedText, String testFileName) {
        String testClassName = testFileName.substring(0, testFileName.lastIndexOf("."));
        String generateClassName = extraClassNameFromGeneratedText(generatedText);
        return generateClassName == null ? generatedText : generatedText.replace(generateClassName, testClassName);
    }

    private static PsiDirectory getCurrentSelectedFilePsiDir(@NotNull Project project) {
        PsiDirectory psiDirectory = null;
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (virtualFile != null) {
                VirtualFile parent = virtualFile.getParent();
                psiDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(parent);
            }
        }
        return psiDirectory;
    }
}
