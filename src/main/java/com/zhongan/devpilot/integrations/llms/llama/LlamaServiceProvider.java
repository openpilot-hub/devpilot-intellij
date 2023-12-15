package com.zhongan.devpilot.integrations.llms.llama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotFailedResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessResponse;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.UserAgentUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@Service(Service.Level.PROJECT)
public final class LlamaServiceProvider implements LlmProvider {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Call call;

    @Override
    public String chatCompletion(DevPilotChatCompletionRequest chatCompletionRequest) {
        var host = CodeLlamaSettingsState.getInstance().getModelHost();

        if (StringUtils.isEmpty(host)) {
            return "Chat completion failed: host is empty";
        }

        var modelName = CodeLlamaSettingsState.getInstance().getModelName();

        if (StringUtils.isEmpty(modelName)) {
            return "Chat completion failed: code llama model name is empty";
        }

        chatCompletionRequest.setModel(modelName);

        okhttp3.Response response;

        try {
            var request = new Request.Builder()
                    .url(host + "/v1/chat/completions")
                    .header("User-Agent", UserAgentUtils.getUserAgent())
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

    private String parseResult(DevPilotChatCompletionRequest chatCompletionRequest, okhttp3.Response response) throws IOException {
        if (response == null) {
            return DevPilotMessageBundle.get("devpilot.chatWindow.response.null");
        }

        var result = Objects.requireNonNull(response.body()).string();

        if (response.isSuccessful()) {
            var message = objectMapper.readValue(result, DevPilotSuccessResponse.class)
                    .getChoices()
                    .get(0)
                    .getMessage();
            // multi chat message
            var devPilotMessage = new DevPilotMessage();
            devPilotMessage.setRole("assistant");
            devPilotMessage.setContent(message.getContent());
            chatCompletionRequest.getMessages().add(devPilotMessage);
            return message.getContent();

        } else {
            return objectMapper.readValue(result, DevPilotFailedResponse.class)
                    .getError()
                    .getMessage();
        }
    }
}
