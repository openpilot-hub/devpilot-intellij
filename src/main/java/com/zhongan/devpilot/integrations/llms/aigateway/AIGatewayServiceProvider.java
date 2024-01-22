package com.zhongan.devpilot.integrations.llms.aigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotFailedResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessResponse;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.UserAgentUtils;
import com.zhongan.devpilot.util.ZaSsoUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.zhongan.devpilot.constant.DefaultConst.AI_GATEWAY_INSTRUCT_COMPLETION;
import static com.zhongan.devpilot.constant.DefaultConst.AI_GATEWAY_INSTRUCT_COMPLETION_ACCESS_KEY_TEMP;

@Service(Service.Level.PROJECT)
public final class AIGatewayServiceProvider implements LlmProvider {

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Call call;

    @Override
    public String chatCompletion(DevPilotChatCompletionRequest chatCompletionRequest) {
        var ssoEnum = ZaSsoUtils.getSsoEnum();

        if (!ZaSsoUtils.isLogin(ssoEnum)) {
            return "Chat completion failed: please login <a href=\"" + ZaSsoUtils.getZaSsoAuthUrl(ssoEnum) + "\">" + ssoEnum.getDisplayName() + "</a>";
        }

        var selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        var host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);

        if (StringUtils.isEmpty(host)) {
            return "Chat completion failed: host is empty";
        }

        var modelTypeEnum = ModelTypeEnum.fromName(selectedModel);
        chatCompletionRequest.setModel(modelTypeEnum.getCode());

        okhttp3.Response response;

        try {
            var request = new Request.Builder()
                    .url(host + "/devpilot/v1/chat/completions")
                    .header("User-Agent", UserAgentUtils.getUserAgent())
                    .header("Auth-Type", ZaSsoUtils.getSsoType())
                    .post(RequestBody.create(objectMapper.writeValueAsString(chatCompletionRequest), MediaType.parse("application/json")))
                    .build();

            call = client.newCall(request);
            response = call.execute();
        } catch (Exception e) {
            return "Chat completion failed: " + e.getMessage();
        }

        try {
            return parseResult(chatCompletionRequest, response);
        } catch (Exception e) {
            return "Chat completion failed: " + e.getMessage();
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

            call = client.newCall(request);
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

    private String parseResult(okhttp3.Response response) throws IOException {
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
