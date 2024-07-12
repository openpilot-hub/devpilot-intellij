package com.zhongan.devpilot.network;

import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.enums.LoginTypeEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.AvailabilityCheck;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
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
        if (!LoginUtils.isLogin()) {
            DevPilotNotification.notLoginNotification(project);
        } else if (!isNetWorkAvailable()) {
            DevPilotNotification.netWorkDownNotification(project);
        } else {
            DevPilotStatusBarBaseWidget.update(project, LoginUtils.isLogin() ? DevPilotStatusEnum.LoggedIn : DevPilotStatusEnum.NotLoggedIn);
        }
    }

    private boolean isNetWorkAvailable() {
        String url = getCheckUrl();
        OkHttpClient client = OkhttpUtils.getClient();
        Request request = new Request.Builder().url(url).build();
        try {
            client.newCall(request).execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getCheckUrl() {
        var setting = DevPilotLlmSettingsState.getInstance();
        var loginType = LoginTypeEnum.getLoginTypeEnum(setting.getLoginType());
        switch (loginType) {
            case WX:
                return "https://devpilot.zhongan.com";
            case ZA:
            case ZA_TI:
                return AIGatewaySettingsState.getInstance().getModelBaseHost(null);
            default:
                return "";
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
