package com.zhongan.devpilot.util;

import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;

public class UserAgentUtils {
    public static String getUserAgent() {
        // format: idea version|plugin version|uuid
        return String.format("%s|%s|%s", DevPilotVersion.getIdeaVersion(),
                DevPilotVersion.getDevPilotVersion(), DevPilotLlmSettingsState.getInstance().getUuid());
    }
}
