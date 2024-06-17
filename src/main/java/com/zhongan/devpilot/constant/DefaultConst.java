package com.zhongan.devpilot.constant;

import com.zhongan.devpilot.util.ConfigBundleUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

public class DefaultConst {

    private DefaultConst() {
    }

    public static final String GPT_35_MAX_TOKEN_EXCEPTION_MSG = DevPilotMessageBundle.get("devpilot.chatWindow.context.overflow");

    public static final int GPT_35_TOKEN_MAX_LENGTH = 16384;

    public static final int CONVERSATION_WINDOW_LENGTH = 8;

    public static final String DEFAULT_SOURCE_STRING = "JetBrains IDE";

    public static final String AI_GATEWAY_INSTRUCT_COMPLETION = "/devpilot/v1/code-completion/default";

    public static final String TELEMETRY_HOST = ConfigBundleUtils.getConfig("devpilot.telemetry.host", "http://localhost:8080");

    public static final String TELEMETRY_LIKE_PATH = "/devpilot/v1/conversation-messages/%s";

    public static final String TELEMETRY_CHAT_ACCEPT_PATH = "/devpilot/v1/conversation-messages/%s/accepted";

    public static final String TELEMETRY_COMPLETION_ACCEPT_PATH = "/devpilot/v1/completion-messages/%s";

    public static final String LOGIN_AUTH_URL = ConfigBundleUtils.getConfig("devpilot.login-h5.host", "http://localhost:8080") + "/login?backUrl=%s&source=%s";

    public static final String LOGIN_CALLBACK_URL = "http://127.0.0.1:%s/login/auth/callback";

    public static final String TRIAL_DEFAULT_HOST = ConfigBundleUtils.getConfig("devpilot.trial.host", "http://localhost:8080");

    public static final String TRIAL_DEFAULT_MODEL = "azure/gpt-3.5-turbo";

    public static final String AI_GATEWAY_DEFAULT_HOST = ConfigBundleUtils.getConfig("devpilot.ai-gateway.host", "http://localhost:8080");

    public static final String RAG_DEFAULT_HOST = ConfigBundleUtils.getConfig("devpilot.rag.host", "http://localhost:8080") + "/devpilot/v1/rag/git_repo/embedding_info/";

    public static final boolean AUTH_ON = true;

    public static final boolean TELEMETRY_ON = true;
}