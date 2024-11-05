package com.zhongan.devpilot.actions.editor;

import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

public class GenerateMethodCommentAction extends SelectedCodeGenerateBaseAction {

    @Override
    protected String getPrompt() {
        return EditorActionEnum.GENERATE_METHOD_COMMENTS.getPrompt();
    }

    @Override
    protected EditorActionEnum getEditorActionEnum() {
        return EditorActionEnum.GENERATE_METHOD_COMMENTS;
    }

    @Override
    protected String getShowText() {
        return DevPilotMessageBundle.get("devpilot.inlay.shortcut.methodComments");
    }

    @Override
    protected void handleValidResult(String result) {

    }
}
