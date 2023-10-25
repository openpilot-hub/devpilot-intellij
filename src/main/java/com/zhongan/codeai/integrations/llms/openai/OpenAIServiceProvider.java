package com.zhongan.codeai.integrations.llms.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.zhongan.codeai.integrations.llms.LlmProvider;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIFailedResponse;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;
import com.zhongan.codeai.integrations.llms.entity.CodeAISuccessResponse;
import com.zhongan.codeai.settings.state.OpenAISettingsState;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@Service(Service.Level.PROJECT)
public final class OpenAIServiceProvider implements LlmProvider {

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Call call;

    @Override
    public String chatCompletion(CodeAIChatCompletionRequest chatCompletionRequest) {
        okhttp3.Response response;

        try {
            var request = new Request.Builder()
                .url(OpenAISettingsState.getInstance().getOpenAIBaseHost() + "/v1/chat/completions")
                .post(RequestBody.create(objectMapper.writeValueAsString(chatCompletionRequest), MediaType.parse("application/json")))
                .build();

            call = client.newCall(request);
            response = call.execute();
        } catch (Exception e) {
            return "Chat completion failed: " + e.getMessage();
        }

        try {
            return parseResult(chatCompletionRequest, response);
        } catch (IOException e) {
            return "Chat completion failed: " + e.getMessage();
        }
    }

    @Override
    public void interruptSend() {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private String parseResult(CodeAIChatCompletionRequest chatCompletionRequest, okhttp3.Response response) throws IOException {
        if (response == null) {
            return "Nothing to see here.";
        }

        String result = Objects.requireNonNull(response.body()).string();

        if (response.isSuccessful()) {
            CodeAISuccessResponse.Message message = objectMapper.readValue(result, CodeAISuccessResponse.class)
                    .getChoices()
                    .get(0)
                    .getMessage();
            //todo 多轮会话处理
            CodeAIMessage codeAIMessage = new CodeAIMessage();
            codeAIMessage.setRole("assistant");
            codeAIMessage.setContent(message.getContent());
            chatCompletionRequest.getMessages().add(codeAIMessage);
            return message.getContent();

        } else {
            return objectMapper.readValue(result, CodeAIFailedResponse.class)
                .getError()
                .getMessage();
        }
    }

}
