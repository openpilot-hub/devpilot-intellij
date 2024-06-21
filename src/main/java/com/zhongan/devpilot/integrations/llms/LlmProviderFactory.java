package com.zhongan.devpilot.integrations.llms;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.enums.LoginTypeEnum;
import com.zhongan.devpilot.integrations.llms.aigateway.AIGatewayServiceProvider;
import com.zhongan.devpilot.integrations.llms.trial.TrialServiceProvider;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;

@Service
public final class LlmProviderFactory {

    public LlmProvider getLlmProvider(Project project) {
        var settings = DevPilotLlmSettingsState.getInstance();
        var loginType = LoginTypeEnum.getLoginTypeEnum(settings.getLoginType());

        switch (loginType) {
            case ZA:
            case ZA_TI:
                return project.getService(AIGatewayServiceProvider.class);
            case WX:
                return project.getService(TrialServiceProvider.class);
        }

        return project.getService(AIGatewayServiceProvider.class);
    }

}
