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

    public static final String AI_GATEWAY_INSTRUCT_COMPLETION = "/devpilot/v2/code-completion/default";

    public static final String TELEMETRY_HOST = ConfigBundleUtils.getConfig("devpilot.telemetry.host", "http://localhost:8085");

    public static final String TELEMETRY_LIKE_PATH = "/devpilot/v1/conversation-messages/%s";

    public static final String TELEMETRY_CHAT_ACCEPT_PATH = "/devpilot/v1/conversation-messages/%s/accepted";

    public static final String TELEMETRY_COMPLETION_ACCEPT_PATH = "/devpilot/v1/completion-messages/%s";

    public static final String LOGIN_AUTH_URL = ConfigBundleUtils.getConfig("devpilot.login-h5.host", "http://localhost:8085") + "/login?backUrl=%s&source=%s";

    public static final String LOGIN_CALLBACK_URL = "http://127.0.0.1:%s/login/auth/callback";

    public static final String TRIAL_DEFAULT_HOST = ConfigBundleUtils.getConfig("devpilot.trial.host", "http://localhost:8085");

    public static final String TRIAL_DEFAULT_MODEL = "azure/gpt-3.5-turbo";

    public static final String AI_GATEWAY_DEFAULT_HOST = ConfigBundleUtils.getConfig("devpilot.ai-gateway.host", "http://localhost:8085");

    public static final String RAG_DEFAULT_HOST = ConfigBundleUtils.getConfig("devpilot.rag.host", "http://localhost:8085") + "/devpilot/v1/rag/git_repo/embedding_info/";

    public static final String OFFICIAL_WEBSITE_URL = ConfigBundleUtils.getConfig("devpilot.official.website.host", "http://localhost:8085");

    public static final String REMOTE_RAG_DEFAULT_HOST = ConfigBundleUtils.getConfig("devpilot.remote.rag.host", "http://localhost") + ":";

    public static final String REMOTE_RAG_DEFAULT_PATH = "/rag";

    public static final String EMBEDDING_SUBMIT_PATH = "/submitChunks";

    public static final String EMBEDDING_DELETE_PATH = "/deleteChunks";

    public static final String EMBEDDING_SEARCH_PATH = "/local-rag";

    public static final String EMBEDDING_RESET_INDEX_PATH = "/reset-index";

    public static final String AGENT_INSTRUCT_COMPLETION = "/instruct-completion";

    public static final String FEEDBACK_URL = OFFICIAL_WEBSITE_URL + "/feedback";

    public static final String PROFILE_URL = OFFICIAL_WEBSITE_URL + "/profile";

    public static final String AUTH_INFO_BUILD_TEMPLATE = "authType=%s&token=%s&userId=%s&timestamp=%s";

    public static final boolean AUTH_ON = true;

    public static final boolean TELEMETRY_ON = true;

    public static final boolean REQUEST_ENCODING_ON = true;

    public static final int COMPLETION_TRIGGER_INTERVAL = 1000;

    public static final int CHAT_STEP_ONE = 1;

    public static final int CHAT_STEP_TWO = 2;

    public static final int CHAT_STEP_THREE = 3;

    public static final String DEFAULT_PROMPT_VERSION = "V250102";

    public static final String CODE_PREDICT_PROMPT_VERSION = "V250102";

    public static final String GIT_COMMIT_PROMPT_VERSION = "V250102";

    public static final String D2C_PROMPT_VERSION = "V250206";

    public static final int NORMAL_CHAT_TYPE = 1;

    public static final int SMART_CHAT_TYPE = 2;

}