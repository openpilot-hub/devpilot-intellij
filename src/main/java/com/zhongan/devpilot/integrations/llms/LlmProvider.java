package com.zhongan.devpilot.integrations.llms;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;

public interface LlmProvider {

    String chatCompletion(DevPilotChatCompletionRequest chatCompletionRequest);

    String instructCompletion(DevPilotInstructCompletionRequest instructCompletionRequest);

    void interruptSend();

}
