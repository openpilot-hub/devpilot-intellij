package com.zhongan.devpilot.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Utils {
    public static String base64Encoding(String code) {
        return Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Decoding(String code) {
        return new String(Base64.getDecoder().decode(code), StandardCharsets.UTF_8);
    }
}
