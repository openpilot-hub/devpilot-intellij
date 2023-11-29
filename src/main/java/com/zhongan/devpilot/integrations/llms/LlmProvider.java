package com.zhongan.devpilot.integrations.llms;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;

public interface LlmProvider {

    String chatCompletion(DevPilotChatCompletionRequest chatCompletionRequest);

    void interruptSend();

}
