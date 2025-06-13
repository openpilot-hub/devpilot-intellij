package com.zhongan.devpilot.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.zhongan.devpilot.agents.BinaryManager;

import java.util.Map;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.zhongan.devpilot.constant.DefaultConst.EVENT_BUS_PATH;
import static com.zhongan.devpilot.constant.DefaultConst.REMOTE_AGENT_DEFAULT_HOST;

public class EventUtil {
    private static final Logger LOG = Logger.getInstance(EventUtil.class);

    /**
     * 发送事件请求到远程代理
     *
     * @param eventData 事件数据
     * @return 是否发送成功
     */
    public static boolean sendEventRequest(Map<String, Object> eventData) {
        try {
            Pair<Integer, Long> portPId = BinaryManager.INSTANCE.retrieveAlivePort();
            if (null != portPId) {
                String url = REMOTE_AGENT_DEFAULT_HOST + portPId.first + EVENT_BUS_PATH;
                var request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", UserAgentUtils.buildUserAgent())
                        .header("Auth-Type", LoginUtils.getLoginType())
                        .post(RequestBody.create(JsonUtils.toJson(eventData), MediaType.parse("application/json")))
                        .build();

                Call call = OkhttpUtils.getClient().newCall(request);
                try (Response response = call.execute()) {
                    LOG.info("Send event." + response);
                    return response.isSuccessful();
                }
            }
        } catch (Exception e) {
            LOG.warn("Exception occurred while sending event request.", e);
        }
        return false;
    }
}
