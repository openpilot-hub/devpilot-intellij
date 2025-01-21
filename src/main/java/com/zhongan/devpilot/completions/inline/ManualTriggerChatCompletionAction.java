package com.zhongan.devpilot.completions.inline;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.zhongan.devpilot.completions.general.DependencyContainer;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.enums.CompletionTypeEnum;
import com.zhongan.devpilot.listener.DevPilotLineIconListener;

import org.jetbrains.annotations.NotNull;

public class ManualTriggerChatCompletionAction extends BaseCodeInsightAction implements InlineCompletionAction {

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @Override
            public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
                if (editor.getCaretModel().getCaretCount() > 1) {
                    return;
                }

                if (project != editor.getProject()) {
                    return;
                }

                int line = editor.getCaretModel().getLogicalPosition().line;
                var gutterIconRenderer = DevPilotLineIconListener.updateGutterIcon(editor, line);

                gutterIconRenderer.setLoading(true);

                DevPilotCompletion lastShownCompletion = CompletionPreview.getCurrentCompletion(editor);
                DependencyContainer.singletonOfInlineCompletionHandler().retrieveAndShowCompletion(
                        editor, editor.getCaretModel().getOffset(), lastShownCompletion, "",
                        new DefaultCompletionAdjustment(), CompletionTypeEnum.CHAT_COMPLETION.getType(), gutterIconRenderer
                );
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    @Override
    protected boolean isValidForLookup() {
        return true;
    }

}