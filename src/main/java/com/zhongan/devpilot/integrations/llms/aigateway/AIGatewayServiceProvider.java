package com.zhongan.devpilot.integrations.llms.aigateway;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotFailedResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessResponse;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.EditorUtils;
import com.zhongan.devpilot.util.GitUtil;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.UserAgentUtils;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.io.IOException;
import java.util.HashMap;
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
import static com.zhongan.devpilot.util.VirtualFileUtil.getRelativeFilePath;

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

        try {
            var requestBuilder = new Request.Builder()
                    .url(host + "/devpilot/v1/chat/completions")
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .header("Auth-Type", LoginUtils.getLoginType());

            if (isLatestUserContentContainsRepo(chatCompletionRequest)) {
                String repoName = EditorUtils.getCurrentEditorRepositoryName(project);
                if (repoName != null && GitUtil.isRepoEmbedded(repoName)) {
                    requestBuilder.header("Embedded-Repos-V2", repoName);
                    requestBuilder.header("X-B3-Language", LanguageSettingsState.getInstance().getLanguageIndex() == 1 ? "zh-CN" : "en-US");
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

        int offset = instructCompletionRequest.getOffset();
        Editor editor = instructCompletionRequest.getEditor();
        final Document[] document = new Document[1];
        final Language[] language = new Language[1];
        final VirtualFile[] virtualFile = new VirtualFile[1];

        ApplicationManager.getApplication().runReadAction(() -> {
            document[0] = editor.getDocument();
            language[0] = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(document[0]).getLanguage();
            virtualFile[0] = FileDocumentManager.getInstance().getFile(document[0]);
        });

        String text = document[0].getText();
        String relativePath = getRelativeFilePath(editor.getProject(), virtualFile[0]);

        Map<String, String> map = new HashMap<>();
        map.put("document", text);
        map.put("position", String.valueOf(offset));
        map.put("language", language[0].getID());
        map.put("filePath", relativePath);
        map.put("completionType", instructCompletionRequest.getCompletionType());
        ObjectMapper objectMapper = new ObjectMapper();

        okhttp3.Response response;
        String json;
        try {
            json = objectMapper.writeValueAsString(map);
            var request = new Request.Builder()
                .url(host + AI_GATEWAY_INSTRUCT_COMPLETION)
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
                if (!StringUtils.isEmpty(content) && content.startsWith("@repo")) {
                    messages.get(i).setContent(content.substring(5));
                    return Boolean.TRUE;
                }
                return false;
            }
        }
        return false;
    }
}
