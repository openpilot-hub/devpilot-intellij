package com.zhongan.devpilot.action;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.util.MarkdownUtil;
import org.junit.Assert;

import java.util.List;
import java.util.regex.Pattern;

public class GenerateTestsTest extends BasePlatformTestCase {

    private static final String code = "public static boolean isBlank(String str) {\n" +
            "        int strLen;\n" +
            "        if (str == null || (strLen = str.length()) == 0) {\n" +
            "            return true;\n" +
            "        }\n" +
            "        for (int i = 0; i < strLen; i++) {\n" +
            "            if ((Character.isWhitespace(str.charAt(i)) == false)) {\n" +
            "                return false;\n" +
            "            }\n" +
            "        }\n" +
            "        return true;\n" +
            "    }";

    public void testGenerateTestsFormat() {
        String prompt = EditorActionEnum.GENERATE_TESTS.getPrompt();
        String newPrompt = prompt.replace("{{selectedCode}}", code);
        String unitTest = sendMessage(newPrompt);
        List<String> blocks = MarkdownUtil.divideMarkdown(unitTest);
        boolean hasCodeBlock = blocks.stream().anyMatch(s -> s.startsWith("```"));
        Assert.assertTrue(hasCodeBlock);
    }

    public void testResponseInChinese() {
        String prompt = EditorActionEnum.GENERATE_TESTS.getPrompt();
        String newPrompt = prompt.replace("{{selectedCode}}", code) + "Please response in Chinese.";
        String unitTest = sendMessage(newPrompt);
        List<String> blocks = MarkdownUtil.divideMarkdown(unitTest);
        boolean allNoCodeBlocksContainChinese = blocks.stream()
                .filter(s -> !s.startsWith("```"))
                .allMatch(this::containsChinese);
        Assert.assertTrue(allNoCodeBlocksContainChinese);
    }

    private String sendMessage(String message) {
        var devPilotMessage = new DevPilotMessage();
        devPilotMessage.setRole("user");
        devPilotMessage.setContent(message);

        DevPilotChatCompletionRequest devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        devPilotChatCompletionRequest.getMessages().add(devPilotMessage);

        LlmProvider llmProvider = new LlmProviderFactory().getLlmProvider(getProject());
        return llmProvider.chatCompletion(devPilotChatCompletionRequest);
    }

    private boolean containsChinese(String s) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        return p.matcher(s).find();
    }

}
