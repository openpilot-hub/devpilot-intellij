package com.zhongan.devpilot.network;

import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.AvailabilityCheck;
import com.zhongan.devpilot.statusBar.DevPilotStatusBarBaseWidget;
import com.zhongan.devpilot.statusBar.status.DevPilotStatusEnum;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.OkhttpUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class DevPilotStatusChecker implements Runnable {

    private final Project project;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public DevPilotStatusChecker(@NotNull Project project) {
        this.project = project;
    }

    private void checkAndNotify() {
        String modelBaseHost = AIGatewaySettingsState.getInstance().getModelBaseHost(null);
        if (!isUrlReachable(modelBaseHost)) {
            DevPilotNotification.netWorkDownNotification(project);
        } else if (!LoginUtils.isLogin()) {
            DevPilotNotification.notLoginNotification(project);
        } else {
            DevPilotStatusBarBaseWidget.update(project, LoginUtils.isLogin() ? DevPilotStatusEnum.LoggedIn : DevPilotStatusEnum.NotLoggedIn);
        }
    }

    private boolean isUrlReachable(String url) {
        OkHttpClient client = OkhttpUtils.getClient();
        Request request = new Request.Builder().url(url).build();
        try {
            client.newCall(request).execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void run() {
        if (AvailabilityCheck.getInstance().getEnable()) {
            checkAndNotify();
        }
    }

    public void checkNetworkAndLogStatus() {
        if (AvailabilityCheck.getInstance().getEnable()) {
            executor.scheduleAtFixedRate(this, 0, 2, TimeUnit.HOURS);
        }
    }

}
