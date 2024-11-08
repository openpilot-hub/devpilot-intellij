package com.zhongan.devpilot.integrations.llms.trial;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotFailedResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotRagRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotRagResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessResponse;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.GatewayRequestUtils;
import com.zhongan.devpilot.util.GatewayRequestV2Utils;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.MessageModel;
import com.zhongan.devpilot.webview.model.RecallModel;

import java.io.IOException;
import java.util.List;
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
import static com.zhongan.devpilot.constant.DefaultConst.TRIAL_DEFAULT_HOST;

@Service(Service.Level.PROJECT)
public final class TrialServiceProvider implements LlmProvider {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private EventSource es;

    private DevPilotChatToolWindowService toolWindowService;

    private MessageModel resultModel = new MessageModel();

    @Override
    public String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest,
                                 Consumer<String> callback, List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int chatType) {
        var service = project.getService(DevPilotChatToolWindowService.class);
        this.toolWindowService = service;

        if (!LoginUtils.isLogin()) {
            service.callErrorInfo("Chat completion failed: please login");
            DevPilotNotification.linkInfo("Please Login", "Account", LoginUtils.loginUrl());
            return "";
        }

        try {
            var requestBody = GatewayRequestV2Utils.encodeRequest(chatCompletionRequest);
            if (requestBody == null) {
                service.callErrorInfo("Chat completion failed: request body is null");
                return "";
            }

            var request = new Request.Builder()
                    .url(TRIAL_DEFAULT_HOST + "/v2/chat/completions")
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .header("Auth-Type", "wx")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            this.es = this.buildEventSource(request, service, callback, remoteRefs, localRefs, chatType);
        } catch (Exception e) {
            service.callErrorInfo("Chat completion failed: " + e.getMessage());
            return "";
        }

        return "";
    }

    @Override
    public DevPilotChatCompletionResponse chatCompletionSync(DevPilotChatCompletionRequest chatCompletionRequest) {
        if (!LoginUtils.isLogin()) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: please login <a href=\"" + LoginUtils.loginUrl() + "\">Wechat Login</a>");
        }

        Response response;

        try {
            var requestBody = GatewayRequestV2Utils.encodeRequest(chatCompletionRequest);
            if (requestBody == null) {
                return DevPilotChatCompletionResponse.failed("Chat completion failed: request body is null");
            }

            var request = new Request.Builder()
                    .url(TRIAL_DEFAULT_HOST + "/v2/chat/completions")
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .header("Auth-Type", "wx")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            var call = OkhttpUtils.getClient().newCall(request);
            response = call.execute();
        } catch (Exception e) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }

        try {
            return parseResult(chatCompletionRequest, response);
        } catch (Exception e) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }
    }

    @Override
    public DevPilotMessage instructCompletion(DevPilotInstructCompletionRequest instructCompletionRequest) {
        if (!LoginUtils.isLogin()) {
            DevPilotNotification.infoAndAction("Instruct completion failed: please login", "", LoginUtils.loginUrl());
            return null;
        }

        Response response;
        String json = GatewayRequestUtils.completionRequestJson(instructCompletionRequest);
        try {
            var request = new Request.Builder()
                    .url(TRIAL_DEFAULT_HOST + AI_GATEWAY_INSTRUCT_COMPLETION)
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .header("Auth-Type", LoginUtils.getLoginType())
                    .header("X-B3-Language", LanguageSettingsState.getInstance().getLanguageIndex() == 1 ? "zh-CN" : "en-US")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            Call call = OkhttpUtils.getClient().newCall(request);
            response = call.execute();
        } catch (Exception e) {
            Logger.getInstance(getClass()).warn("Instruct completion failed: " + e.getMessage());
            return null;
        }

        try {
            return parseResponse(response);
        } catch (Exception e) {
            Logger.getInstance(getClass()).warn("Instruct completion failed: " + e.getMessage());
            return null;
        }
    }

    private DevPilotMessage parseResponse(Response response) {
        DevPilotMessage devPilotMessage = null;
        try (response) {
            String responseBody = response.body().string();
            Gson gson = new Gson();
            devPilotMessage = gson.fromJson(responseBody, DevPilotMessage.class);
        } catch (IOException e) {
            Logger.getInstance(getClass()).warn("Parse completion response failed: " + e.getMessage());
        }
        return devPilotMessage;
    }

    @Override
    public void interruptSend() {
        if (es != null) {
            es.cancel();
            // remember the broken message
            if (resultModel != null && !StringUtils.isEmpty(resultModel.getContent())) {
                resultModel.setStreaming(false);
                var recall = resultModel.getRecall();
                if (recall != null) {
                    var newRecall = RecallModel.createTerminated(3, recall.getRemoteRefs(), recall.getLocalRefs());
                    resultModel.setRecall(newRecall);
                }
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
            LoginUtils.logout();
            return DevPilotChatCompletionResponse.failed("Chat completion failed: Unauthorized, please login <a href=\"" + LoginUtils.loginUrl() + "\">Wechat Login</a>");
        } else {
            return DevPilotChatCompletionResponse.failed(objectMapper.readValue(result, DevPilotFailedResponse.class)
                    .getError()
                    .getMessage());
        }
    }

    @Override
    public DevPilotChatCompletionResponse codePrediction(DevPilotChatCompletionRequest chatCompletionRequest) {
        if (!LoginUtils.isLogin()) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: please login <a href=\"" + LoginUtils.loginUrl() + "\">Wechat Login</a>");
        }

        Response response;

        try {
            var requestBody = GatewayRequestV2Utils.encodeRequest(chatCompletionRequest);
            if (requestBody == null) {
                return DevPilotChatCompletionResponse.failed("Chat completion failed: request body is null");
            }

            var request = new Request.Builder()
                    .url(TRIAL_DEFAULT_HOST + "/v2/chat/completions")
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .header("Auth-Type", "wx")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            var call = OkhttpUtils.getClient().newCall(request);
            response = call.execute();
        } catch (Exception e) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }

        try {
            return parseResult(chatCompletionRequest, response);
        } catch (Exception e) {
            return DevPilotChatCompletionResponse.failed("Chat completion failed: " + e.getMessage());
        }
    }

    @Override
    public List<DevPilotRagResponse> ragCompletion(DevPilotRagRequest ragRequest) {
        return null;
    }
}
