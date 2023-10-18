package com.zhongan.codeai.integrations.llms;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.codeai.integrations.llms.openai.OpenAIServiceProvider;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;

@Service
public class LlmProviderFactory {

    public LlmProvider getLlmProvider(Project project) {
        if (CodeAILlmSettingsState.getInstance().isUseOpenAIService()) {
            return project.getService(OpenAIServiceProvider.class);
        }
        return null;
    }

}
