package com.zhongan.devpilot.integrations.llms;

import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.entity.*;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public interface LlmProvider {

    String chatCompletion(Project project, DevPilotChatCompletionRequest chatCompletionRequest, Consumer<String> callback);

    DevPilotChatCompletionResponse chatCompletionSync(DevPilotChatCompletionRequest chatCompletionRequest);

    String instructCompletion(DevPilotInstructCompletionRequest var1);

    void interruptSend();

    default void restoreMessage(MessageModel messageModel) {
        // default not restore message
    }

    default EventSource buildEventSource(Request request,
                                         DevPilotChatToolWindowService service, Consumer<String> callback) {
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

                var choice = response.getChoices().get(0);
                var finishReason = choice.getFinishReason();

                if (choice.getDelta().getContent() != null) {
                    result.append(choice.getDelta().getContent());
                }

                var streaming = !"stop".equals(finishReason);

                var assistantMessage = MessageModel
                        .buildAssistantMessage(response.getId(), time, result.toString(), streaming);

                restoreMessage(assistantMessage);
                service.callWebView(assistantMessage);

                if (!streaming) {
                    service.addMessage(assistantMessage);
                    var devPilotMessage = new DevPilotMessage();
                    devPilotMessage.setId(response.getId());
                    devPilotMessage.setRole("assistant");
                    devPilotMessage.setContent(result.toString());
                    service.addRequestMessage(devPilotMessage);
                    if (callback != null) {
                        callback.accept(result.toString());
                    }
                }
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                var message = "Chat completion failed";

                if (response != null && response.code() == 401) {
                    message = "Chat completion failed: Unauthorized";
                }

                if (t != null) {
                    if (t.getMessage().contains("Socket closed")) {
                        return;
                    }

                    message = "Chat completion failed: " + t.getMessage();
                }

                var assistantMessage = MessageModel
                        .buildAssistantMessage(UUID.randomUUID().toString(), time, message, false);

                service.callWebView(assistantMessage);
                service.addMessage(assistantMessage);
            }
        });
    }
}
