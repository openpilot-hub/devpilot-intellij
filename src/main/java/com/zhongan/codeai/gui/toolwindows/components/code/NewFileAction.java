package com.zhongan.codeai.gui.toolwindows.components.code;

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
import com.zhongan.codeai.actions.notifications.CodeAINotification;
import com.zhongan.codeai.enums.EditorActionEnum;
import com.zhongan.codeai.util.BalloonAlertUtils;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

public class NewFileAction implements ActionListener {

    private final Editor editor;

    private final String fileExtension;

    private final Project project;

    private final EditorActionEnum actionType;

    private final Editor chosenEditor;

    public NewFileAction(Editor editor, String fileExtension, Project project, EditorActionEnum actionType, Editor chosenEditor) {
        this.editor = editor;
        this.fileExtension = fileExtension;
        this.project = project;
        this.actionType = actionType;
        this.chosenEditor = chosenEditor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String generatedText = getGeneratedText();

        if (actionType == EditorActionEnum.GENERATE_TESTS && ".java".equals(fileExtension)) {
            handleGenerateTestsAction(generatedText);
        } else {
            handleDefaultAction(generatedText);
        }
    }

    private String getGeneratedText() {
        return this.editor.getSelectionModel().hasSelection() ?
                this.editor.getSelectionModel().getSelectedText() : this.editor.getDocument().getText();
    }

    private void handleGenerateTestsAction(String generatedText) {
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(chosenEditor.getDocument());
        if (virtualFile != null) {
            String testFileDirPath = virtualFile.getParent().getPath().replace("main", "test");
            PsiDirectory testPsiDir = createPsiDirectory(testFileDirPath);

            FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(fileExtension.substring(1));
            String fileName = virtualFile.getNameWithoutExtension();
            String testFileName = fileName + "Test" + fileExtension;

            if (testPsiDir.findFile(testFileName) != null) {
                BalloonAlertUtils.showErrorAlert(CodeAIMessageBundle.get("codeai.alter.file.exist"), 0, -10, Balloon.Position.above);
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
    }

    private PsiDirectory createPsiDirectory(String dirPath) {
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            CodeAINotification.error(CodeAIMessageBundle.get("codeai.notification.create.dir.failed"));
        }
        VirtualFile dirVF = LocalFileSystem.getInstance().refreshAndFindFileByPath(dirPath);
        return dirVF == null ? null : PsiDirectoryFactory.getInstance(project).createDirectory(dirVF);
    }

    private void handleDefaultAction(String generatedText) {
        PsiDirectory selectedFilePsiDir = getCurrentSelectedFilePsiDir(project);
        if (selectedFilePsiDir == null) {
            selectedFilePsiDir = createPsiDirectory(project.getBasePath());
        }

        String fileName = "temp_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        if (".java".equals(fileExtension)) {
            String generatedClassName = extraClassNameFromGeneratedText(generatedText);
            if (generatedClassName != null) {
                fileName = generatedClassName + fileExtension;
            }
        }

        if (selectedFilePsiDir.findFile(fileName) != null) {
            BalloonAlertUtils.showErrorAlert(CodeAIMessageBundle.get("codeai.alter.file.exist"), 0, -10, Balloon.Position.above);
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

    private String extraClassNameFromGeneratedText(String generatedText) {
        String regex = "public class (\\w+) \\{";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(generatedText);

        String generatedClassName = null;
        if (matcher.find()) {
            generatedClassName = matcher.group(1);
        }
        return generatedClassName;
    }

    private String modifyGeneratedTextClassName(String generatedText, String testFileName) {
        String testClassName = testFileName.substring(0, testFileName.lastIndexOf("."));
        String generateClassName = extraClassNameFromGeneratedText(generatedText);
        return generatedText.replace(generateClassName, testClassName);
    }

    private PsiDirectory getCurrentSelectedFilePsiDir(@NotNull Project project) {
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
