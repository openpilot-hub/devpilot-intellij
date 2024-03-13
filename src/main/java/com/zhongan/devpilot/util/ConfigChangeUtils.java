package com.zhongan.devpilot.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.webview.model.ConfigModel;

public class ConfigChangeUtils {
    public static ConfigModel configInit() {
        var language = LanguageSettingsState.getInstance().getLanguageIndex();
        var locale = (language == 1) ? "cn" : "en";
        var username = DevPilotLlmSettingsState.getInstance().getFullName();
        var loggedIn = LoginUtils.isLogin();

        return new ConfigModel(ThemeUtils.themeType(), locale, username, loggedIn);
    }

    public static void themeChanged(Project project) {
        var service = project.getService(DevPilotChatToolWindowService.class);
        service.changeTheme(ThemeUtils.themeType());
    }

    public static void themeChanged() {
        var projects = ProjectManager.getInstance().getOpenProjects();

        for (var project : projects) {
            var service = project.getService(DevPilotChatToolWindowService.class);
            service.changeTheme(ThemeUtils.themeType());
        }
    }

    public static void localeChanged(Project project) {
        var language = LanguageSettingsState.getInstance().getLanguageIndex();
        var locale = (language == 1) ? "cn" : "en";
        var service = project.getService(DevPilotChatToolWindowService.class);
        service.changeLocale(locale);
    }

    public static void localeChanged(int language) {
        var locale = (language == 1) ? "cn" : "en";
        var projects = ProjectManager.getInstance().getOpenProjects();

        for (var project : projects) {
            var service = project.getService(DevPilotChatToolWindowService.class);
            service.changeLocale(locale);
        }
    }

    public static void localeChanged() {
        var language = LanguageSettingsState.getInstance().getLanguageIndex();
        var locale = (language == 1) ? "cn" : "en";
        var projects = ProjectManager.getInstance().getOpenProjects();

        for (var project : projects) {
            var service = project.getService(DevPilotChatToolWindowService.class);
            service.changeLocale(locale);
        }
    }
}
