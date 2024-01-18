package com.zhongan.devpilot.integrations.llms.llama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.util.UserAgentUtils;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;

@Service(Service.Level.PROJECT)
public final class LlamaServiceProvider implements LlmProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EventSource es;

    private DevPilotChatToolWindowService toolWindowService;

    private MessageModel resultModel = new MessageModel();

    @Override
    public String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest, Consumer<String> callback) {
        var host = CodeLlamaSettingsState.getInstance().getModelHost();

        if (StringUtils.isEmpty(host)) {
            return "Chat completion failed: host is empty";
        }

        var modelName = CodeLlamaSettingsState.getInstance().getModelName();

        if (StringUtils.isEmpty(modelName)) {
            return "Chat completion failed: code llama model name is empty";
        }

        chatCompletionRequest.setModel(modelName);

        var service = project.getService(DevPilotChatToolWindowService.class);
        this.toolWindowService = service;

        try {
            var request = new Request.Builder()
                    .url(host + "/v1/chat/completions")
                    .header("User-Agent", UserAgentUtils.getUserAgent())
                    .post(RequestBody.create(objectMapper.writeValueAsString(chatCompletionRequest), MediaType.parse("application/json")))
                    .build();

            this.es = this.buildEventSource(request, service, callback);
        } catch (Exception e) {
            return "Chat completion failed: " + e.getMessage();
        }

        return "";
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
}
