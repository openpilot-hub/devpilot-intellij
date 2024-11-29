package com.zhongan.devpilot.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class NewFileUtils {
    private static final Logger LOG = Logger.getInstance(NewFileUtils.class);

    public static void createNewFile(Project project, String generatedText, CodeReferenceModel codeReferenceModel, String lang) {
        String fileExtension = StringUtils.isEmpty(lang) ? ".java" : MarkdownUtil.getFileExtensionFromLanguage(lang);

        if (StringUtils.equalsIgnoreCase(".java", fileExtension)) {
            String fileUrl = StringUtils.EMPTY;
            if (codeReferenceModel != null) {
                fileUrl = codeReferenceModel.getFileUrl();
                var fileName = codeReferenceModel.getFileName();
                if (codeReferenceModel.getType() == EditorActionEnum.GENERATE_TESTS) {
                    // java test will goto special logic
                    handleGenerateTestsAction(project, generatedText, fileExtension, fileName, fileUrl);
                    return;
                }
            }
            handleDefaultActionForJava(project, generatedText, fileExtension, fileUrl);
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

    private static String handleGeneratedJavaFileName(String generatedText, String fileExtension) {
        String fileName = "temp_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        String generatedClassName = extraClassNameFromGeneratedText(generatedText);
        if (generatedClassName != null) {
            fileName = generatedClassName + fileExtension;
        }
        return fileName;
    }

    private static PsiDirectory handleGeneratedJavaPackageName(Project project, String generatedText, String fileUrl) {
        String result = StringUtils.EMPTY;
        String target = File.separator + "src" + File.separator + "main" + File.separator + "java";
        if (StringUtils.isNotEmpty(fileUrl) && fileUrl.contains(target)) {
            // Find the project root path for the currently selected code
            result = fileUrl.substring(0, fileUrl.indexOf(target));
            if (StringUtils.isNotEmpty(result)) {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(result);
                if (vf == null) {
                    result = StringUtils.EMPTY;
                }
            }
        } else {
            // Find the project root path of the file currently open for editing
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (null != editor) {
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
                if (null != virtualFile) {
                    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
                    VirtualFile rootFile = fileIndex.getContentRootForFile(virtualFile);
                    result = null != rootFile ? rootFile.getPath() : StringUtils.EMPTY;
                }
            }
        }
        // When the root path is obtained, append the sourceDirectory
        if (StringUtils.isNotEmpty(result)) {
            result += target + File.separator;
        }
        if (StringUtils.isEmpty(result)) {
            // Try to determine whether the target exists in the root path. If the target exists, use it directly
            result = project.getBasePath();
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(result + target);
            if (null != vf) {
                result += target + File.separator;
            } else {
                result += File.separator;
            }
        }
        // Append PackageDirectory
        String generatedPackageName = extractPackageFromGeneratedText(generatedText);
        if (generatedPackageName != null) {
            result += StringUtils.replace(generatedPackageName, ".", File.separator);
        }
        return createPsiDirectory(project, result);
    }

    private static void handleDefaultActionForJava(Project project, String generatedText, String fileExtension, String fileUrl) {
        String fileName = handleGeneratedJavaFileName(generatedText, fileExtension);
        PsiDirectory targetPsiDir = handleGeneratedJavaPackageName(project, generatedText, fileUrl);
        openAndWriteFile(project, generatedText, fileExtension, targetPsiDir, fileName);
    }

    private static PsiDirectory handleGeneratedPsiDir(Project project) {
        PsiDirectory selectedFilePsiDir = getCurrentSelectedFilePsiDir(project);
        if (selectedFilePsiDir == null) {
            selectedFilePsiDir = createPsiDirectory(project, project.getBasePath());
        }
        return selectedFilePsiDir;
    }

    private static void handleDefaultAction(Project project, String generatedText, String fileExtension) {
        String fileName = "temp_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        PsiDirectory targetPsiDir = handleGeneratedPsiDir(project);
        openAndWriteFile(project, generatedText, fileExtension, targetPsiDir, fileName);
    }

    private static void openAndWriteFile(Project project, String generatedText, String fileExtension, PsiDirectory targetFilePsiDir, String fileName) {
        if (targetFilePsiDir.findFile(fileName) != null) {
            BalloonAlertUtils.showErrorAlert(DevPilotMessageBundle.get("devpilot.alter.file.exist"), 0, -10, Balloon.Position.above);
            FileEditorManager.getInstance(project).openFile(targetFilePsiDir.findFile(fileName).getVirtualFile(), true);
            return;
        }

        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(fileExtension.substring(1));
        PsiDirectory finalSelectedFileDir = targetFilePsiDir;
        String finalFileName = fileName;

        DumbService.getInstance(project).runWhenSmart(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(finalFileName, fileType, generatedText);
                PsiFile createdFile = (PsiFile) finalSelectedFileDir.add(fileFromText);
                FileEditorManager.getInstance(project).openFile(createdFile.getVirtualFile(), true);
            });
        });
    }

    private static PsiDirectory createPsiDirectory(Project project, String dirPath) {
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            String message = DevPilotMessageBundle.get("devpilot.notification.create.dir.failed");
            DevPilotNotification.error(message);
            LOG.warn(message, e);
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

    private static String extractPackageFromGeneratedText(String generatedText) {
        String regex = "package\\s+([\\w.]+);";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(generatedText);

        String packageName = null;
        if (matcher.find()) {
            packageName = matcher.group(1);
        }
        return packageName;
    }

}
