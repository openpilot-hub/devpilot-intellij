package com.zhongan.devpilot.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RequestV2Utils {
    public static String encodeRequest(Object requestBody) throws Exception {
        String jsonString = JsonUtils.toJson(requestBody);

        if (jsonString == null) {
            return null;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        }
        byte[] compressed = byteArrayOutputStream.toByteArray();

        return Base64.getEncoder().encodeToString(compressed);
    }

    public static <T> T decodeRequest(String encodedRequest, Class<T> valueType) throws Exception {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedRequest);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
        }

        String jsonString = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        return JsonUtils.fromJson(jsonString, valueType);
    }
}
