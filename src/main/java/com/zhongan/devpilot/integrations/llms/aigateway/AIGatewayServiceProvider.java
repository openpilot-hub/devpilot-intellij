package com.zhongan.devpilot.integrations.llms.aigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotFailedResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessResponse;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;
import com.zhongan.devpilot.util.ZaSsoUtils;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;

import static com.zhongan.devpilot.constant.DefaultConst.AI_GATEWAY_INSTRUCT_COMPLETION;
import static com.zhongan.devpilot.constant.DefaultConst.AI_GATEWAY_INSTRUCT_COMPLETION_ACCESS_KEY_TEMP;

@Service(Service.Level.PROJECT)
public final class AIGatewayServiceProvider implements LlmProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EventSource es;

    private DevPilotChatToolWindowService toolWindowService;

    private MessageModel resultModel = new MessageModel();

    @Override
    public String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest, Consumer<String> callback) {
        var service = project.getService(DevPilotChatToolWindowService.class);
        this.toolWindowService = service;

        var ssoEnum = ZaSsoUtils.getSsoEnum();
        if (!ZaSsoUtils.isLogin(ssoEnum)) {
            service.callErrorInfo("Chat completion failed: please login");
            DevPilotNotification.linkInfo("Please Login", ssoEnum.getDisplayName(), ZaSsoUtils.getZaSsoAuthUrl(ssoEnum));
            return "";
        }

        var selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        var host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);

        if (StringUtils.isEmpty(host)) {
            service.callErrorInfo("Chat completion failed: host is empty");
            return "";
        }

        var modelTypeEnum = ModelTypeEnum.fromName(selectedModel);
        chatCompletionRequest.setModel(modelTypeEnum.getCode());

        try {
            var request = new Request.Builder()
                    .url(host + "/devpilot/v1/chat/completions")
                    .header("User-Agent", UserAgentUtils.getUserAgent())
                    .header("Auth-Type", ZaSsoUtils.getSsoType())
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

    @Override
    public void handleNoAuth(DevPilotChatToolWindowService service) {
        var ssoEnum = ZaSsoUtils.getSsoEnum();
        ZaSsoUtils.logout(ssoEnum);
        service.callErrorInfo("Chat completion failed: No auth, please login");
        DevPilotNotification.linkInfo("Please Login", ssoEnum.getDisplayName(), ZaSsoUtils.getZaSsoAuthUrl(ssoEnum));
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

        Response response;

        try {
            var request = new Request.Builder()
                .url(host + "/devpilot/v1/chat/completions")
                .header("User-Agent", UserAgentUtils.getUserAgent())
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

    private DevPilotChatCompletionResponse parseResult(DevPilotChatCompletionRequest chatCompletionRequest, Response response) throws IOException {
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

        } else if (response.code() == 401) {
            var ssoEnum = ZaSsoUtils.getSsoEnum();
            ZaSsoUtils.logout(ssoEnum);
            return DevPilotChatCompletionResponse.failed("Chat completion failed: Unauthorized, please login <a href=\"" + ZaSsoUtils.getZaSsoAuthUrl(ssoEnum) + "\">" + ssoEnum.getDisplayName() + "</a>");
        } else {
            return DevPilotChatCompletionResponse.failed(objectMapper.readValue(result, DevPilotFailedResponse.class)
                .getError()
                .getMessage());
        }
    }

    @Override
    public String instructCompletion(DevPilotInstructCompletionRequest instructCompletionRequest) {
        var ssoEnum = ZaSsoUtils.getSsoEnum();

        if (!ZaSsoUtils.isLogin(ssoEnum)) {
            DevPilotNotification.infoAndAction("Instruct completion failed: please login", ssoEnum.getDisplayName(), ZaSsoUtils.getZaSsoAuthUrl(ssoEnum));
            return null;
        }

        var selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        var host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);

        if (StringUtils.isEmpty(host)) {
            Logger.getInstance(getClass()).warn("Instruct completion failed: host is empty");
            return null;
        }

        okhttp3.Response response;

        try {
            var request = new Request.Builder()
                .url(host + AI_GATEWAY_INSTRUCT_COMPLETION)
                .header("User-Agent", UserAgentUtils.getUserAgent())
                .header("Auth-Type", ZaSsoUtils.getSsoType())
                //TODO删除
                .header("access-key", AI_GATEWAY_INSTRUCT_COMPLETION_ACCESS_KEY_TEMP)
                .post(RequestBody.create(objectMapper.writeValueAsString(instructCompletionRequest), MediaType.parse("application/json")))
                .build();
            Call call = OkhttpUtils.getClient().newCall(request);
            response = call.execute();
        } catch (Exception e) {
            Logger.getInstance(getClass()).warn("Chat completion failed: " + e.getMessage());
            return null;
        }

        try {
            return parseResult(response);
        } catch (Exception e) {
            Logger.getInstance(getClass()).warn("Chat completion failed: " + e.getMessage());
            return null;
        }
    }

    private String parseResult(Response response) throws IOException {
        if (response == null) {
            return DevPilotMessageBundle.get("devpilot.chatWindow.response.null");
        }

        var result = Objects.requireNonNull(response.body()).string();

        if (response.isSuccessful()) {
            List<Map> message = (List<Map>) objectMapper.readValue(result, Map.class).get("choices");
            String content = (String) message.get(0).get("text");
            // multi chat message
            var devPilotMessage = new DevPilotMessage();
            devPilotMessage.setRole("assistant");
            devPilotMessage.setContent(content);
            return content;

        } else if (response.code() == 401) {
            var ssoEnum = ZaSsoUtils.getSsoEnum();
            ZaSsoUtils.logout(ssoEnum);
            return "Chat completion failed: Unauthorized, please login <a href=\"" + ZaSsoUtils.getZaSsoAuthUrl(ssoEnum) + "\">" + ssoEnum.getDisplayName() + "</a>";
        } else {
            return objectMapper.readValue(result, DevPilotFailedResponse.class)
                .getError()
                .getMessage();
        }
    }

}
