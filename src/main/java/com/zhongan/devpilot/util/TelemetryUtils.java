package com.zhongan.devpilot.util;

import com.intellij.psi.PsiFile;
import com.zhongan.devpilot.enums.ChatActionTypeEnum;
import com.zhongan.devpilot.webview.model.CodeActionModel;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.zhongan.devpilot.constant.DefaultConst.TELEMETRY_CHAT_ACCEPT_PATH;
import static com.zhongan.devpilot.constant.DefaultConst.TELEMETRY_COMPLETION_ACCEPT_PATH;
import static com.zhongan.devpilot.constant.DefaultConst.TELEMETRY_HOST;
import static com.zhongan.devpilot.constant.DefaultConst.TELEMETRY_LIKE_PATH;
import static com.zhongan.devpilot.constant.DefaultConst.TELEMETRY_ON;

public class TelemetryUtils {

    public static void messageFeedback(String id, boolean action) {
        if (!isTelemetryTurnOn()) {
            return;
        }

        var feedbackRequest = new FeedbackRequest(action);
        var requestJson = JsonUtils.toJson(feedbackRequest);

        if (requestJson == null) {
            return;
        }

        var path = String.format(TELEMETRY_LIKE_PATH, id);
        var url = TELEMETRY_HOST + path;

        sendMessage(url, requestJson);
    }

    public static void chatAccept(CodeActionModel codeActionModel, ChatActionTypeEnum actionType) {
        if (!isTelemetryTurnOn()) {
            return;
        }

        chatAccept(codeActionModel.getMessageId(), codeActionModel.getContent(), codeActionModel.getLang(), actionType);
    }

    public static void chatAccept(String id, String acceptLines, String language, ChatActionTypeEnum actionType) {
        // if language is null return text
        if (StringUtils.isEmpty(language)) {
            language = "text";
        }

        var chatAcceptRequest = new ChatAcceptRequest(acceptLines,
                language.toLowerCase(Locale.ROOT), actionType.getType());
        var requestJson = JsonUtils.toJson(chatAcceptRequest);

        if (requestJson == null) {
            return;
        }

        var path = String.format(TELEMETRY_CHAT_ACCEPT_PATH, id);
        var url = TELEMETRY_HOST + path;

        sendMessage(url, requestJson);
    }

    public static void completionAccept(String id, PsiFile file) {
        if (!isTelemetryTurnOn()) {
            return;
        }

        var name = file.getName();
        var fileExtension = name.substring(name.lastIndexOf(".") + 1);

        var lang = LanguageUtil.getLanguageByExtension(fileExtension);

        String language = null;

        if (lang != null) {
            language = lang.getLanguageName();
        }

        completionAccept(id, language);
    }

    public static void completionAccept(String id, String language) {
        if (!isTelemetryTurnOn()) {
            return;
        }

        // if language is null return text
        if (StringUtils.isEmpty(language)) {
            language = "text";
        }

        var completionAcceptRequest = new CompletionAcceptRequest(language.toLowerCase(Locale.ROOT));
        var requestJson = JsonUtils.toJson(completionAcceptRequest);

        if (requestJson == null) {
            return;
        }

        var path = String.format(TELEMETRY_COMPLETION_ACCEPT_PATH, id);
        var url = TELEMETRY_HOST + path;

        sendMessage(url, requestJson);
    }

    public static void sendMessage(String url, String requestJson) {
        var client = OkhttpUtils.getClient();

        try {
            var request = new Request.Builder()
                    .url(url)
                    .header("Auth-Type", LoginUtils.getLoginType())
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .put(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    // ignore failure
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            // ignore error
        }
    }

    public static boolean isTelemetryTurnOn() {
        return TELEMETRY_ON;
    }

    static class FeedbackRequest {
        private Boolean agreeStatus;

        FeedbackRequest(Boolean agreeStatus) {
            this.agreeStatus = agreeStatus;
        }

        public Boolean getAgreeStatus() {
            return agreeStatus;
        }

        public void setAgreeStatus(Boolean agreeStatus) {
            this.agreeStatus = agreeStatus;
        }
    }

    static class ChatAcceptRequest {
        private String acceptedLines;

        private String language;

        private String actionType;

        ChatAcceptRequest(String acceptedLines, String language, String actionType) {
            this.acceptedLines = acceptedLines;
            this.language = language;
            this.actionType = actionType;
        }

        public String getAcceptedLines() {
            return acceptedLines;
        }

        public void setAcceptedLines(String acceptedLines) {
            this.acceptedLines = acceptedLines;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }
    }

    static class CompletionAcceptRequest {
        private String language;

        CompletionAcceptRequest(String language) {
            this.language = language;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}
