package com.zhongan.devpilot.integrations.llms.aigateway;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.*;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

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
        var service = project.getService(DevPilotChatToolWindowService.class);
        this.toolWindowService = service;

        if (StringUtils.isEmpty(host)) {
            service.callErrorInfo("Chat completion failed: host is empty");
            return "";
        }

        var modelTypeEnum = ModelTypeEnum.fromName(selectedModel);
        chatCompletionRequest.setModel(modelTypeEnum.getCode());

        try {
            var request = new Request.Builder()
                    .url(host + "/v1/chat/completions")
                    .header("User-Agent", parseUserAgent())
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

    @Override
    public DevPilotChatCompletionResponse chatCompletionSync(DevPilotChatCompletionRequest chatCompletionRequest) {
        var selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        var host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);

        if (StringUtils.isEmpty(host)) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: host is empty");
        }

        var modelTypeEnum = ModelTypeEnum.fromName(selectedModel);
        chatCompletionRequest.setModel(modelTypeEnum.getCode());

        okhttp3.Response response;

        try {
            var request = new Request.Builder()
                    .url(host + "/v1/chat/completions")
                    .header("User-Agent", parseUserAgent())
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

    public String instructCompletion(DevPilotInstructCompletionRequest instructCompletionRequest) {
        String selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        String host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);
        if (StringUtils.isEmpty(host)) {
            Logger.getInstance(this.getClass()).warn("Instruct completion failed: host is empty");
            return null;
        } else {
            Response response;
            try {
                var modelTypeEnum = ModelTypeEnum.fromName(selectedModel);
                instructCompletionRequest.setModel(modelTypeEnum.getCode());
                Request request = (new Request.Builder()).url(host + "/v1/chat/completions").header("User-Agent", UserAgentUtils.getUserAgent()).post(RequestBody.create(this.objectMapper.writeValueAsString(instructCompletionRequest), MediaType.parse("application/json"))).build();
                Call call = OkhttpUtils.getClient().newCall(request);
                response = call.execute();
            } catch (Exception var9) {
                Logger.getInstance(this.getClass()).warn("Chat completion failed: " + var9.getMessage());
                return null;
            }

            try {
                return this.parseCompletionsResult(response);
            } catch (Exception var8) {
                Logger.getInstance(this.getClass()).warn("Chat completion failed: " + var8.getMessage());
                return null;
            }
        }
    }

    private String parseCompletionsResult(Response response) throws IOException {
        if (response == null) {
            return DevPilotMessageBundle.get("devpilot.chatWindow.response.null");
        } else {
            String result = ((ResponseBody) Objects.requireNonNull(response.body())).string();
            if (response.isSuccessful()) {
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                var message = objectMapper.readValue(result.substring(result.indexOf('{')), DevPilotSuccessResponse.class)
                        .getChoices()
                        .get(0)
                        .getMessage().getContent();
                // 正则表达式提取```{语言名称}\n(content)```中content的内容
                String regex = "```[^`]+?\\n+?(.*?)```";

                Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
                Matcher matcher = pattern.matcher(message);

                if (matcher.find()) {
                    message = matcher.group(1);
                } else {
                    message = "";
                }
                return message;
            } else {
//                DevPilotNotification.debug("SSO Type:" + var10000 + ", Status Code:" + response.code() + ".");
                if (response.code() == 401) {
                } else {
                    Object var6 = this.objectMapper.readValue(result, DevPilotFailedResponse.class);
                    DevPilotNotification.debug("Error message: [" + ((DevPilotFailedResponse) var6).getError().getMessage() + "].");
                }
                return "";
            }
        }
    }

}
