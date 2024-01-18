package com.zhongan.devpilot.integrations.llms.aigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;

@Service(Service.Level.PROJECT)
public final class AIGatewayServiceProvider implements LlmProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EventSource es;

    private DevPilotChatToolWindowService toolWindowService;

    private MessageModel resultModel = new MessageModel();

    @Override
    public String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest, Consumer<String> callback) {
        var selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        var host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);

        if (StringUtils.isEmpty(host)) {
            return "Chat completion failed: host is empty";
        }

        var modelTypeEnum = ModelTypeEnum.fromName(selectedModel);
        chatCompletionRequest.setModel(modelTypeEnum.getCode());

        var service = project.getService(DevPilotChatToolWindowService.class);
        this.toolWindowService = service;

        try {
            var request = new Request.Builder()
                .url(host + "/devpilot/v1/chat/completions")
                .header("User-Agent", parseUserAgent())
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

    private String parseUserAgent() {
        // format: idea version|plugin version|uuid
        return String.format("%s|%s|%s", DevPilotVersion.getIdeaVersion(),
            DevPilotVersion.getDevPilotVersion(), DevPilotLlmSettingsState.getInstance().getUuid());
    }

}
