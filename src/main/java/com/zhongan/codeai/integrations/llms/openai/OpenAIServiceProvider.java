package com.zhongan.codeai.integrations.llms.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.zhongan.codeai.integrations.llms.LlmProvider;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIFailedResponse;
import com.zhongan.codeai.integrations.llms.entity.CodeAISuccessResponse;
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
            return "Chat completion failed: " + e.getMessage();
        }

        try {
            return parseResult(response);
        } catch (JsonProcessingException e) {
            return "Chat completion failed: " + e.getMessage();
        }
    }

    private String parseResult(HttpResponse<String> response) throws JsonProcessingException {
        if (response == null) {
            return "Nothing to see here.";
        }

        String result = response.body();

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(result, CodeAISuccessResponse.class)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } else {
            return objectMapper.readValue(result, CodeAIFailedResponse.class)
                    .getError()
                    .getMessage();
        }
    }
}
