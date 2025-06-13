package com.zhongan.devpilot.provider.ut.java;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.enums.UtFrameTypeEnum;
import com.zhongan.devpilot.provider.ut.UtFrameworkProvider;

public class DefaultJavaUtFrameworkProvider implements UtFrameworkProvider {
    
    public static final DefaultJavaUtFrameworkProvider INSTANCE = new DefaultJavaUtFrameworkProvider();
    
    private DefaultJavaUtFrameworkProvider() {
    }
    
    @Override
    public UtFrameTypeEnum getUTFramework(Project project, Editor editor) {
        return UtFrameTypeEnum.JUNIT5_MOCKITO;
    }
}