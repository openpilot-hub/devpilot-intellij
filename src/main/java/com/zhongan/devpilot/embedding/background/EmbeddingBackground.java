package com.zhongan.devpilot.embedding.background;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.embedding.LocalEmbeddingService;

import org.jetbrains.annotations.NotNull;

public class EmbeddingBackground extends Task.Backgroundable {
    public EmbeddingBackground(Project project) {
        super(project, "Embedding project", true);
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        LocalEmbeddingService.wrapIndexTask(super.getProject(), LocalEmbeddingService::indexProject);
    }
}
