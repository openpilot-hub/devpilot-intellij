package com.zhongan.devpilot.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.ModelType;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.constant.PromptConst;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;

import java.util.ArrayList;
import java.util.List;

public class TokenUtils {

    private static final Encoding GPT_3_5_TURBO_16K_ENC = Encodings.newDefaultEncodingRegistry()
                                                                    .getEncodingForModel(ModelType.GPT_3_5_TURBO_16K);

    public static List<Integer> ComputeTokensFromMessagesUsingGPT35Enc(List<DevPilotMessage> messages) {
        List<Integer> tokensCount = new ArrayList<>(messages.size());
        for (DevPilotMessage message : messages) {
            tokensCount.add(GPT_3_5_TURBO_16K_ENC.countTokensOrdinary(message.getContent()));
        }
        return tokensCount;
    }

    public static Integer ComputeTokensFromContentUsingGTP35Enc(String content) {
        return GPT_3_5_TURBO_16K_ENC.countTokensOrdinary(content);
    }

    /**
     * check length of input rather than max limit
     */
    public static boolean isInputExceedLimit(String content, String prompt) {
        // text too long, openai server always timeout
        String userPrompt = prompt.replace("{{selectedCode}}", content);

        return TokenUtils.ComputeTokensFromContentUsingGTP35Enc(userPrompt) +
                TokenUtils.ComputeTokensFromContentUsingGTP35Enc(PromptConst.RESPONSE_FORMAT) > DefaultConst.GPT_35_TOKEN_MAX_LENGTH;
    }

    public static boolean isInputExceedLimit(String userPrompt) {
        // text too long, openai server always timeout
        return TokenUtils.ComputeTokensFromContentUsingGTP35Enc(userPrompt) +
                TokenUtils.ComputeTokensFromContentUsingGTP35Enc(PromptConst.RESPONSE_FORMAT) > DefaultConst.GPT_35_TOKEN_MAX_LENGTH;
    }

}
