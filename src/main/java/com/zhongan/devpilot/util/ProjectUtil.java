package com.zhongan.devpilot.util;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.ComponentUtil;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class ProjectUtil {

    public static @Nullable Project getCurrentContextProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) {
            return null;
        }
        if (openProjects.length == 1) {
            return openProjects[0];
        }

        Project project = null;
        if (!GraphicsEnvironment.isHeadless()) {
            Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            if (window == null) {
                window = ComponentUtil.getActiveWindow();
            }

            Component component = ComponentUtil.findUltimateParent(window);
            if (component instanceof IdeFrame) {
                project = ((IdeFrame) component).getProject();
            }
        }

        return isValidProject(project) ? project : null;
    }

    private static boolean isValidProject(@Nullable Project project) {
        return project != null && !project.isDisposed() && !project.isDefault() && project.isInitialized();
    }

    public static boolean isSandboxProject() {
        return StringUtils.contains(PathManager.getSystemPath(), "sandbox");
    }
}
