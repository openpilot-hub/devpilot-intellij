package com.zhongan.devpilot.update;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.openapi.util.BuildNumber;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class DevPilotUpdate  {

    public static void installUpdate(Project project) {
        UpdateSettings settingsCopy = new UpdateSettings();
        settingsCopy.getState().copyFrom(UpdateSettings.getInstance().getState());
        settingsCopy.getState().setCheckNeeded(true);
        settingsCopy.getState().setPluginsCheckNeeded(true);
        settingsCopy.getState().setThirdPartyPluginsAllowed(true);
        settingsCopy.getState().setShowWhatsNewEditor(false);
        UpdateChecker.updateAndShowResult(project, settingsCopy);
    }

    public static final class DevPilotUpdateTask extends Task.Backgroundable {
        public DevPilotUpdateTask(Project project) {
            super(project, "Check DevPilot Update", false);
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                checkUpdate(getProject(), indicator);
            } catch (Exception e) {
                Logger.getInstance(getClass()).warn("Check update failed: " + e.getMessage());
            }
        }
    }

    private static void checkUpdate(Project project, ProgressIndicator indicator) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var pluginId = PluginId.getId("com.zhongan.devPilot");

        var getInternalPluginUpdatesMethod =
                UpdateChecker.class.getMethod(
                        "getInternalPluginUpdates", BuildNumber.class, ProgressIndicator.class);
        var internalPluginUpdates = getInternalPluginUpdatesMethod.invoke(null, null, indicator);
        var getPluginUpdatesMethod = internalPluginUpdates.getClass().getMethod("getPluginUpdates");
        var pluginUpdates = getPluginUpdatesMethod.invoke(internalPluginUpdates);
        var getAllEnabledMethod = pluginUpdates.getClass().getMethod("getAllEnabled");
        var allEnabled = getAllEnabledMethod.invoke(pluginUpdates);

        var list = (Collection<PluginDownloader>) allEnabled;

        boolean shouldUpdate = list.stream().anyMatch((item) -> pluginId.equals(item.getId()));

        if (shouldUpdate) {
            DevPilotNotification.updateNotification(project);
        }
    }
}
