package com.zhongan.devpilot.integrations.llms.llama;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotFailedResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessResponse;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;

@Service(Service.Level.PROJECT)
public final class LlamaServiceProvider implements LlmProvider {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private EventSource es;

    private DevPilotChatToolWindowService toolWindowService;

    private MessageModel resultModel = new MessageModel();

    @Override
    public String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest, Consumer<String> callback) {
        var host = CodeLlamaSettingsState.getInstance().getModelHost();
        var service = project.getService(DevPilotChatToolWindowService.class);
        this.toolWindowService = service;

        if (StringUtils.isEmpty(host)) {
            service.callErrorInfo("Chat completion failed: host is empty");
            return "";
        }

        var modelName = CodeLlamaSettingsState.getInstance().getModelName();

        if (StringUtils.isEmpty(modelName)) {
            service.callErrorInfo("Chat completion failed: code llama model name is empty");
            return "";
        }

        chatCompletionRequest.setModel(modelName);

        try {
            var request = new Request.Builder()
                .url(host + "/v1/chat/completions")
                .header("User-Agent", UserAgentUtils.buildUserAgent())
                .post(RequestBody.create(objectMapper.writeValueAsString(chatCompletionRequest), MediaType.parse("application/json")))
                .build();

            this.es = this.buildEventSource(request, service, callback);
        } catch (Exception e) {
            service.callErrorInfo("Chat completion failed: " + e.getMessage());
            return "";
        }

        return "";
    }

    @Override
    public DevPilotMessage instructCompletion(DevPilotInstructCompletionRequest instructCompletionRequest) {
        return null;
    }

    @Override
    public void interruptSend() {
        if (es != null) {
            es.cancel();
            // remember the broken message
            if (resultModel != null && !StringUtils.isEmpty(resultModel.getContent())) {
                resultModel.setStreaming(false);
                toolWindowService.addMessage(resultModel);
            }

            toolWindowService.callWebView();
            // after interrupt, reset result model
            resultModel = null;
        }
    }

    @Override
    public void restoreMessage(MessageModel messageModel) {
        this.resultModel = messageModel;
    }

    @Override
    public DevPilotChatCompletionResponse chatCompletionSync(DevPilotChatCompletionRequest chatCompletionRequest) {
        var host = CodeLlamaSettingsState.getInstance().getModelHost();

        if (StringUtils.isEmpty(host)) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: host is empty");
        }

        var modelName = CodeLlamaSettingsState.getInstance().getModelName();

        if (StringUtils.isEmpty(modelName)) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: code llama model name is empty");
        }

        chatCompletionRequest.setModel(modelName);

        okhttp3.Response response;

        try {
            var request = new Request.Builder()
                .url(host + "/v1/chat/completions")
                .header("User-Agent", UserAgentUtils.buildUserAgent())
                .post(RequestBody.create(objectMapper.writeValueAsString(chatCompletionRequest), MediaType.parse("application/json")))
                .build();

            Call call = OkhttpUtils.getClient().newCall(request);
            response = call.execute();
        } catch (Exception e) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }

        try {
            return parseResult(chatCompletionRequest, response);
        } catch (IOException e) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }
    }

    private DevPilotChatCompletionResponse parseResult(DevPilotChatCompletionRequest chatCompletionRequest, okhttp3.Response response) throws IOException {
        if (response == null) {
            return DevPilotChatCompletionResponse.failed(DevPilotMessageBundle.get("devpilot.chatWindow.response.null"));
        }

        var result = Objects.requireNonNull(response.body()).string();

        if (response.isSuccessful()) {
            var message = objectMapper.readValue(result, DevPilotSuccessResponse.class)
                .getChoices()
                .get(0)
                .getMessage();
            var devPilotMessage = new DevPilotMessage();
            devPilotMessage.setRole("assistant");
            devPilotMessage.setContent(message.getContent());
            chatCompletionRequest.getMessages().add(devPilotMessage);
            return DevPilotChatCompletionResponse.success(message.getContent());

        } else {
            return DevPilotChatCompletionResponse.failed(objectMapper.readValue(result, DevPilotFailedResponse.class)
                .getError()
                .getMessage());
        }
    }

}
