package com.zhongan.devpilot.provider.ut;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.enums.UtFrameTypeEnum;

public interface UtFrameworkProvider {

    UtFrameTypeEnum getUTFramework(Project project, Editor editor);

}
