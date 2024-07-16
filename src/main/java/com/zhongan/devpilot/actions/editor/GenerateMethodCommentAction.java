package com.zhongan.devpilot.actions.editor;

import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

public class GenerateMethodCommentAction extends SelectedCodeGenerateBaseAction {

    @Override
    protected EditorActionEnum getEditorActionEnum() {
        return EditorActionEnum.COMMENT_METHOD;
    }

    @Override
    protected String getShowText() {
        return DevPilotMessageBundle.get("devpilot.inlay.shortcut.methodComments");
    }

    @Override
    protected void handleValidResult(String result) {

    }
}
