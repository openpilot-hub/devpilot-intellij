package com.zhongan.devpilot.completions.inline;

import com.intellij.codeInsight.hint.HintManagerImpl.ActionToIgnore;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;

public class AcceptDevPilotInlineCompletionByLineAction extends EditorAction implements ActionToIgnore, InlineCompletionAction {

    public static final String ACTION_ID = "AcceptDevPilotInlineCompletionByLineAction";

    public AcceptDevPilotInlineCompletionByLineAction() {
        super(new AcceptInlineCompletionHandler());
    }

    private static class AcceptInlineCompletionHandler extends EditorWriteActionHandler {

        @Override
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CompletionPreview.getInstance(editor).applyPreviewByLine(caret != null ? caret : editor.getCaretModel().getCurrentCaret());
        }

        @Override
        protected boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
            return CompletionPreview.getInstance(editor) != null;
        }
    }
}