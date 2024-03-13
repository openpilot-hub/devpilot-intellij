//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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

    public ManualTriggerDevPilotInlineCompletionAction() {
    }

    protected @NotNull CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {

                if (CompletionSettingsState.getInstance().getEnable()) {
                    DevPilotCompletion lastShownCompletion = CompletionPreview.getCurrentCompletion(editor);
                    ManualTriggerDevPilotInlineCompletionAction.this.handler.retrieveAndShowCompletion(editor, editor.getCaretModel().getOffset(), lastShownCompletion, "", new DefaultCompletionAdjustment());
                }
            }

            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    protected boolean isValidForLookup() {
        return true;
    }
}
