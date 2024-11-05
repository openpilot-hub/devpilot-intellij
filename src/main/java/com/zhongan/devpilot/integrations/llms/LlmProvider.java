package com.zhongan.devpilot.integrations.llms;

import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionResponse;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessStreamingResponse;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.MessageModel;
import com.zhongan.devpilot.webview.model.RecallModel;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import static com.zhongan.devpilot.constant.DefaultConst.SMART_CHAT_TYPE;

public interface LlmProvider {

    String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest,
                          Consumer<String> callback, List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int type);

    DevPilotChatCompletionResponse chatCompletionSync(DevPilotChatCompletionRequest chatCompletionRequest);

    DevPilotMessage instructCompletion(DevPilotInstructCompletionRequest instructCompletionRequest);

    DevPilotChatCompletionResponse codePrediction(DevPilotChatCompletionRequest chatCompletionRequest);

    void interruptSend();

    default void restoreMessage(MessageModel messageModel) {
        // default not restore message
    }

    default void handleNoAuth(DevPilotChatToolWindowService service) {
        var content = "Chat completion failed: Auth Failed";
        var assistantMessage = MessageModel.buildInfoMessage(content);

        service.callWebView(assistantMessage);
        service.addMessage(assistantMessage);
    }

    default void handleContextTooLong(DevPilotChatToolWindowService service) {
        var content = DevPilotMessageBundle.get("devpilot.notification.input.tooLong");
        var assistantMessage = MessageModel.buildInfoMessage(content);
        service.callWebView(assistantMessage);
        service.addMessage(assistantMessage);
    }

    default void handlePluginVersionTooLow(DevPilotChatToolWindowService service, boolean callWebView) {
        if (callWebView) {
            var content = DevPilotMessageBundle.get("devpilot.notification.version.message");
            var assistantMessage = MessageModel.buildInfoMessage(content);
            service.callWebView(assistantMessage);
            service.addMessage(assistantMessage);
        }
        DevPilotNotification.upgradePluginNotification(service.getProject());
    }

    default EventSource buildEventSource(Request request, DevPilotChatToolWindowService service, Consumer<String> callback,
                                         List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int chatType) {
        var time = System.currentTimeMillis();
        var result = new StringBuilder();
        var client = OkhttpUtils.getClient();

        return EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                if (data.equals("[DONE]")) {
                    return;
                }

                var response = JsonUtils.fromJson(data, DevPilotSuccessStreamingResponse.class);

                if (response == null) {
                    interruptSend();
                    return;
                }

                boolean streaming = Boolean.TRUE;

                if (null != response.getRag()) {
                    var ragResp = response.getRag();
                    var files = ragResp.getFiles();
                    var app = ragResp.getApp();
                    result.append("\n\n<div class=\"rag-files\" data-repo=\"").append(app).append("\">");
                    for (DevPilotSuccessStreamingResponse.RagFile file : files) {
                        result.append("<div class=\"rag-files-item\">").append(file.getFile()).append("</div>");
                    }
                    result.append("</div>\n\n");
                } else {
                    if (CollectionUtils.isEmpty(response.getChoices())) {
                        return;
                    }
                    var choice = response.getChoices().get(0);
                    var finishReason = choice.getFinishReason();

                    if (choice.getDelta().getContent() != null) {
                        result.append(choice.getDelta().getContent());
                    }

                    if (!StringUtils.isEmpty(finishReason)) {
                        streaming = Boolean.FALSE;

                        if (finishReason.contains("length")) {
                            DevPilotNotification.info(DevPilotMessageBundle.get("devpilot.notification.stop.length"));
                        }

                        if (finishReason.contains("content_filter")) {
                            DevPilotNotification.info(DevPilotMessageBundle.get("devpilot.notification.stop.filterContent"));
                        }
                    }
                }

                RecallModel recallModel = null;

                if (chatType == SMART_CHAT_TYPE) {
                    recallModel = RecallModel.create(3, remoteRefs, localRefs);
                }

                var assistantMessage = MessageModel
                        .buildAssistantMessage(response.getId(), time, result.toString(), streaming, recallModel);

                restoreMessage(assistantMessage);
                service.callWebView(assistantMessage);

                if (!streaming) {
                    if (chatType == SMART_CHAT_TYPE) {
                        recallModel = RecallModel.create(4, remoteRefs, localRefs);
                    }
                    assistantMessage = MessageModel
                            .buildAssistantMessage(response.getId(), time, result.toString(), streaming, recallModel);
                    service.callWebView(assistantMessage);

                    service.addMessage(assistantMessage);
                    var devPilotMessage = new DevPilotMessage();
                    devPilotMessage.setId(response.getId());
                    devPilotMessage.setRole("assistant");
                    devPilotMessage.setContent(result.toString());
                    service.addRequestMessage(devPilotMessage);

                    if (callback != null) {
                        callback.accept(result.toString());
                    }

                    // clear message cache
                    restoreMessage(null);
                }
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                var message = "Chat completion failed";

                if (response != null && response.code() == 401) {
                    handleNoAuth(service);
                    return;
                }
                if (response != null && response.code() == 400) {
                    if (response.body() != null) {
                        String responseBody = null;
                        try {
                            responseBody = response.body().string();
                        } catch (IOException e) {

                        }
                        if ("context length is too long".equals(responseBody)) {
                            handleContextTooLong(service);
                            return;
                        } else if (isPluginVersionTooLowResp(resolveJsonBody(responseBody))) {
                            handlePluginVersionTooLow(service, true);
                            return;
                        }
                    }
                }

                if (t != null) {
                    if (t.getMessage().contains("Socket closed")) {
                        return;
                    }

                    if (t.getMessage().contains("stream was reset: CANCEL")) {
                        return;
                    }

                    if (t.getMessage().contains("Canceled")) {
                        return;
                    }

                    message = "Chat completion failed: " + t.getMessage();
                }

                var assistantMessage = MessageModel
                        .buildAssistantMessage(UUID.randomUUID().toString(), time, message, false, null);

                service.callWebView(assistantMessage);
                service.addMessage(assistantMessage);
            }
        });
    }

    default Entity resolveJsonBody(String body) {
        try {
            return JsonUtils.fromJson(body, Entity.class);
        } catch (Exception e) {
            return null;
        }
    }

    default boolean isPluginVersionTooLowResp(Entity entity) {
        if (entity == null) {
            return false;
        }
        return "plugin version is too low".equals(entity.getMessage());
    }

    class Entity {

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

}
