package com.zhongan.codeai.integrations.llms.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.zhongan.codeai.actions.notifications.CodeAINotification;
import com.zhongan.codeai.integrations.llms.LlmProvider;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.settings.state.OpenAISettingsState;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service(Service.Level.PROJECT)
public final class OpenAIServiceProvider implements LlmProvider {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chatCompletion(CodeAIChatCompletionRequest chatCompletionRequest) {
        HttpResponse<String> response = null;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OpenAISettingsState.getInstance().getOpenAIBaseHost() + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(chatCompletionRequest)))
                .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            CodeAINotification.error("Chat completion failed: " + e.getMessage());
        }
        return response.body();
    }

}
