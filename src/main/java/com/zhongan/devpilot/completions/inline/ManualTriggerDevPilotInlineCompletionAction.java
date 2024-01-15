package com.zhongan.devpilot.completions.inline;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.zhongan.devpilot.completions.general.DependencyContainer;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import org.jetbrains.annotations.NotNull;

public class ManualTriggerDevPilotInlineCompletionAction extends BaseCodeInsightAction implements InlineCompletionAction {
    public static final String ACTION_ID = "ManualTriggerDevPilotInlineCompletionAction";

    private final InlineCompletionHandler handler = DependencyContainer.singletonOfInlineCompletionHandler();

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @Override
            public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
                if (!CompletionSettingsState.getInstance().getEnable()) {
                    return;
                }
                DevPilotCompletion lastShownCompletion = CompletionPreview.getCurrentCompletion(editor);
                handler.retrieveAndShowCompletion(
                        editor, editor.getCaretModel().getOffset(), lastShownCompletion, "",
                        new DefaultCompletionAdjustment()
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