package com.zhongan.devpilot.integrations.llms;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.enums.ModelServiceEnum;
import com.zhongan.devpilot.integrations.llms.aigateway.AIGatewayServiceProvider;
import com.zhongan.devpilot.integrations.llms.llama.LlamaServiceProvider;
import com.zhongan.devpilot.integrations.llms.openai.OpenAIServiceProvider;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;

@Service
public final class LlmProviderFactory {

    public LlmProvider getLlmProvider(Project project) {
        var settings = DevPilotLlmSettingsState.getInstance();
        String selectedModel = settings.getSelectedModel();
        ModelServiceEnum modelServiceEnum = ModelServiceEnum.fromName(selectedModel);

        switch (modelServiceEnum) {
            case OPENAI:
                return project.getService(OpenAIServiceProvider.class);
            case LLAMA:
                return project.getService(LlamaServiceProvider.class);
            case AIGATEWAY:
                return project.getService(AIGatewayServiceProvider.class);
        }

        return project.getService(AIGatewayServiceProvider.class);
    }

}
