package com.zhongan.devpilot.integrations.llms;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.integrations.llms.openai.OpenAIServiceProvider;

@Service
public class LlmProviderFactory {

    public LlmProvider getLlmProvider(Project project) {
        return project.getService(OpenAIServiceProvider.class);
    }

}
