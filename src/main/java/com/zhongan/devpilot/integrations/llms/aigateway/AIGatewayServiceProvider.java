package com.zhongan.devpilot.integrations.llms.aigateway;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import com.zhongan.devpilot.util.EditorUtils;
import com.zhongan.devpilot.util.GitUtil;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;
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

@Service(Service.Level.PROJECT)
public final class AIGatewayServiceProvider implements LlmProvider {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private EventSource es;

    private DevPilotChatToolWindowService toolWindowService;

    private MessageModel resultModel = new MessageModel();

    @Override
    public String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest, Consumer<String> callback) {
        var service = project.getService(DevPilotChatToolWindowService.class);
        this.toolWindowService = service;

        if (!LoginUtils.isLogin()) {
            service.callErrorInfo("Chat completion failed: please login");
            DevPilotNotification.linkInfo("Please Login", "Account", LoginUtils.loginUrl());
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
            var requestBuilder = new Request.Builder()
                    .url(host + "/devpilot/v1/chat/completions")
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .header("Auth-Type", LoginUtils.getLoginType());

            if (isLatestUserContentContainsRepo(chatCompletionRequest)) {
                String repoName = EditorUtils.getCurrentEditorRepositoryName(project);
                if (repoName != null && GitUtil.isRepoEmbedded(repoName)) {
                    requestBuilder.header("Embedded-Repos", repoName);
                    chatCompletionRequest.setModel(null);
                }
            }
            var request = requestBuilder
                    .post(RequestBody.create(objectMapper.writeValueAsString(chatCompletionRequest), MediaType.parse("application/json")))
                    .build();

            DevPilotNotification.debug(LoginUtils.getLoginType() + "---" + UserAgentUtils.buildUserAgent());
            this.es = this.buildEventSource(request, service, callback);
        } catch (Exception e) {
            DevPilotNotification.debug("Chat completion failed: " + e.getMessage());

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
        LoginUtils.logout();
        service.callErrorInfo("Chat completion failed: No auth, please login");
        DevPilotNotification.linkInfo("Please Login", "Account", LoginUtils.loginUrl());
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
            String requestBody = objectMapper.writeValueAsString(chatCompletionRequest);
            DevPilotNotification.debug("Send Request :[" + requestBody + "].");

            var request = new Request.Builder()
                .url(host + "/devpilot/v1/chat/completions")
                .header("User-Agent", UserAgentUtils.buildUserAgent())
                .header("Auth-Type", LoginUtils.getLoginType())
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

            Call call = OkhttpUtils.getClient().newCall(request);
            response = call.execute();
        } catch (Exception e) {
            DevPilotNotification.debug("Chat completion failed: " + e.getMessage());
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }

        try {
            return parseCompletionsResult(chatCompletionRequest, response);
        } catch (IOException e) {
            DevPilotNotification.debug("Chat completion failed: " + e.getMessage());
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }
    }

    private DevPilotChatCompletionResponse parseCompletionsResult(DevPilotChatCompletionRequest chatCompletionRequest, Response response) throws IOException {
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
            LoginUtils.logout();
            return DevPilotChatCompletionResponse.failed("Chat completion failed: Unauthorized, please login <a href=\"" + LoginUtils.loginUrl() + "\">" + "sso" + "</a>");
        } else {
            return DevPilotChatCompletionResponse.failed(objectMapper.readValue(result, DevPilotFailedResponse.class)
                .getError()
                .getMessage());
        }
    }

    @Override
    public DevPilotMessage instructCompletion(DevPilotInstructCompletionRequest instructCompletionRequest) {
        if (!LoginUtils.isLogin()) {
            DevPilotNotification.infoAndAction("Instruct completion failed: please login", "", LoginUtils.loginUrl());
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
                .header("User-Agent", UserAgentUtils.buildUserAgent())
                .header("Auth-Type", LoginUtils.getLoginType())
                .post(RequestBody.create(objectMapper.writeValueAsString(instructCompletionRequest), MediaType.parse("application/json")))
                .build();
            Call call = OkhttpUtils.getClient().newCall(request);
            response = call.execute();
        } catch (Exception e) {
            Logger.getInstance(getClass()).warn("Instruct completion failed: " + e.getMessage());
            return null;
        }

        try {
            return parseCompletionsResult(response);
        } catch (Exception e) {
            Logger.getInstance(getClass()).warn("Instruct completion failed: " + e.getMessage());
            return null;
        }
    }

    private DevPilotMessage parseCompletionsResult(Response response) throws IOException {
        if (response == null) {
            return null;
        }

        var result = Objects.requireNonNull(response.body()).string();

        if (response.isSuccessful()) {
            var map = objectMapper.readValue(result, Map.class);
            var message = (List<Map>) map.get("choices");

            var id = (String) map.get("id");
            var content = (String) message.get(0).get("text");

            // multi chat message
            var devPilotMessage = new DevPilotMessage();
            devPilotMessage.setId(id);
            devPilotMessage.setRole("assistant");
            devPilotMessage.setContent(content);
            return devPilotMessage;
        }
        DevPilotNotification.debug("SSO Type:" + LoginUtils.getLoginType() + ", Status Code:" + response.code() + ".");
        if (response.code() == 401) {
            LoginUtils.logout();
        } else {
            DevPilotNotification.debug("Error message: [" + objectMapper.readValue(result, DevPilotFailedResponse.class).getError().getMessage() + "].");
        }

        return null;
    }

    private Boolean isLatestUserContentContainsRepo(DevPilotChatCompletionRequest chatCompletionRequest) {
        List<DevPilotMessage> messages = chatCompletionRequest.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getRole().equals("user")) {
                String content = messages.get(i).getContent();
                if (content.startsWith("@repo")) {
                    messages.get(i).setContent(content.substring(5));
                    return Boolean.TRUE;
                }
                return false;
            }
        }
        return false;
    }

}
