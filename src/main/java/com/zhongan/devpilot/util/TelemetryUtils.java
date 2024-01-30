package com.zhongan.devpilot.util;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class TelemetryUtils {
    private static final String host = "http://openapi-cloud-pub.zhonganinfo.com/openpilot-hub";

    private static final String likePath = "/devpilot/v1/conversation-messages/%s";

    public static void messageFeedback(String id, boolean action) {
        var client = OkhttpUtils.getClient();
        var feedbackRequest = new FeedbackRequest(action);

        var requestJson = JsonUtils.toJson(feedbackRequest);

        if (requestJson == null) {
            return;
        }

        String path = String.format(likePath, id);

        okhttp3.Response response = null;

        try {
            var request = new Request.Builder()
                    .url(host + path)
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
}
