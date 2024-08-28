package com.zhongan.devpilot.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class EditorUtils {

    // repo和根目录的映射关系，用于打开文件时快速定位到根目录
    private static final Map<String, VirtualFile> repoMapping = new HashMap<>();

    public static void openFileAndSelectLines(@NotNull Project project, String fileUrl,
                                              int startLine, Integer startColumn, int endLine, Integer endColumn) {

        VirtualFile codeFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(fileUrl);
        if (codeFile == null || !codeFile.exists()) {
            BalloonAlertUtils.showErrorAlert(DevPilotMessageBundle.get("devpilot.alter.file.not.exist"), 0, -10, Balloon.Position.above);
            return;
        }

        Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, codeFile), true);
        if (editor != null) {
            if (startColumn == null) {
                startColumn = 0;
            }

            if (endColumn == null) {
                endColumn = 0;
            }

            SelectionModel selectionModel = editor.getSelectionModel();
            int startOffset = editor.getDocument().getLineStartOffset(startLine) + startColumn;
            int endOffset = editor.getDocument().getLineStartOffset(endLine) + endColumn;
            selectionModel.setSelection(startOffset, endOffset);

            ScrollingModel scrollingModel = editor.getScrollingModel();
            scrollingModel.scrollTo(new LogicalPosition(startLine, 0), ScrollType.CENTER);
        }
    }

    public static String getCurrentEditorRepositoryName(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = fileEditorManager.getSelectedFiles()[0];
        String repoName = GitUtil.getRepoNameFromFile(project, currentFile);
        String basePath = project.getBasePath();

        if (basePath != null) {
            VirtualFile baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
            repoMapping.putIfAbsent(repoName, baseDir);
        }

        return repoName;
    }

    public static void openFileByRelativePath(String repo, @NotNull Project project, String relativePath) {
        VirtualFile baseDir = repoMapping.get(repo);

        if (baseDir == null) {
            BalloonAlertUtils.showErrorAlert(DevPilotMessageBundle.get("devpilot.alter.file.not.exist"), 0, -10, Balloon.Position.above);
            return;
        }

        VirtualFile file = baseDir.findFileByRelativePath(relativePath);
        if (file == null || !file.exists()) {
            BalloonAlertUtils.showErrorAlert(DevPilotMessageBundle.get("devpilot.alter.file.not.exist"), 0, -10, Balloon.Position.above);
            return;
        }

        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, file), true);
    }
}
