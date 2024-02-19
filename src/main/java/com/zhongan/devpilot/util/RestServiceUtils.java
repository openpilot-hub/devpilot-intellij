package com.zhongan.devpilot.util;

import com.zhongan.devpilot.webview.model.CallbackUserInfo;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RestServiceUtils {
    public static String getParameter(QueryStringDecoder queryStringDecoder, String key) {
        var list = queryStringDecoder.parameters().get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public static CallbackUserInfo parseToken(String token) {
        var decodedBytes = Base64.getDecoder().decode(token);
        var decodedString = new String(decodedBytes);
        var decodedUrl = URLDecoder.decode(decodedString, StandardCharsets.UTF_8);

        return JsonUtils.fromJson(decodedUrl, CallbackUserInfo.class);
    }
}
