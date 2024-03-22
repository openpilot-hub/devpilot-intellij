package com.zhongan.devpilot.util;

import com.intellij.psi.PsiFile;
import com.zhongan.devpilot.enums.ChatActionTypeEnum;
import com.zhongan.devpilot.webview.model.CodeActionModel;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class TelemetryUtils {
    private static final String host = "https://devpilot.zhongan.com/hub";

//    private static final String host = "http://openpilot-hub-foundation.test.za.biz";

    private static final String likePath = "/devpilot/v1/conversation-messages/%s";

    private static final String chatAcceptPath = "/devpilot/v1/conversation-messages/%s/accepted";

    private static final String completionAcceptPath = "/devpilot/v1/completion-messages/%s";

    public static void messageFeedback(String id, boolean action) {
        var feedbackRequest = new FeedbackRequest(action);
        var requestJson = JsonUtils.toJson(feedbackRequest);

        if (requestJson == null) {
            return;
        }

        var path = String.format(likePath, id);
        var url = host + path;

        sendMessage(url, requestJson);
    }

    public static void chatAccept(CodeActionModel codeActionModel, ChatActionTypeEnum actionType) {
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

        var path = String.format(chatAcceptPath, id);
        var url = host + path;

        sendMessage(url, requestJson);
    }

    public static void completionAccept(String id, PsiFile file) {
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
        // if language is null return text
        if (StringUtils.isEmpty(language)) {
            language = "text";
        }

        var completionAcceptRequest = new CompletionAcceptRequest(language.toLowerCase(Locale.ROOT));
        var requestJson = JsonUtils.toJson(completionAcceptRequest);

        if (requestJson == null) {
            return;
        }

        var path = String.format(completionAcceptPath, id);
        var url = host + path;

        sendMessage(url, requestJson);
    }

    public static void sendMessage(String url, String requestJson) {
        var client = OkhttpUtils.getClient();

        okhttp3.Response response = null;

        try {
            var request = new Request.Builder()
                    .url(url)
                    .header("Auth-Type", LoginUtils.getLoginType())
                    .header("User-Agent", UserAgentUtils.buildUserAgent())
                    .put(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .build();

            var call = client.newCall(request);
            response = call.execute();
        } catch (IOException e) {
            // ignore error
        } finally {
            if (response != null) {
                response.close();
            }
        }
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
