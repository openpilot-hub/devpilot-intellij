package com.zhongan.codeai.integrations.llms;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.codeai.integrations.llms.openai.OpenAIServiceProvider;

@Service
public class LlmProviderFactory {

    public LlmProvider getLlmProvider(Project project) {
        return project.getService(OpenAIServiceProvider.class);
    }

}
