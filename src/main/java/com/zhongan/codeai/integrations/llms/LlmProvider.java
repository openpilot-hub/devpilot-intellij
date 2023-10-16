package com.zhongan.codeai.integrations.llms;

import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;

public interface LlmProvider {

    String chatCompletion(CodeAIChatCompletionRequest chatCompletionRequest);

    void interruptSend();

}
