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

import org.jetbrains.annotations.NotNull;

public class EditorUtils {

    public static void openFileAndSelectLines(@NotNull Project project, String fileUrl, int startLine, int endLine) {

        VirtualFile codeFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(fileUrl);
        if (codeFile == null || !codeFile.exists()) {
            BalloonAlertUtils.showErrorAlert(DevPilotMessageBundle.get("devpilot.alter.file.not.exist"), 0, -10, Balloon.Position.above);
            return;
        }

        Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, codeFile), true);
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            int startOffset = editor.getDocument().getLineStartOffset(startLine - 1);
            int endOffset = editor.getDocument().getLineEndOffset(endLine - 1);
            selectionModel.setSelection(startOffset, endOffset);

            ScrollingModel scrollingModel = editor.getScrollingModel();
            scrollingModel.scrollTo(new LogicalPosition(startLine, 0), ScrollType.CENTER);
        }
    }
}
