//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;

public class AcceptDevPilotInlineCompletionAction extends EditorAction implements HintManagerImpl.ActionToIgnore, InlineCompletionAction {
    public static final String ACTION_ID = "AcceptDevPilotInlineCompletionAction";

    public AcceptDevPilotInlineCompletionAction() {
        super(new AcceptInlineCompletionHandler());
    }

    private static class AcceptInlineCompletionHandler extends EditorWriteActionHandler {
        private AcceptInlineCompletionHandler() {
        }

        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            CompletionPreview.getInstance(editor).applyPreview(caret != null ? caret : editor.getCaretModel().getCurrentCaret());
        }

        protected boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
            return CompletionPreview.getInstance(editor) != null;
        }
    }
}
