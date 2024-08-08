package com.zhongan.devpilot.util;

import java.util.Base64;

public class Base64Utils {
    public static String base64Encoding(String code) {
        return Base64.getEncoder().encodeToString(code.getBytes());
    }

    public static String base64Decoding(String code) {
        return new String(Base64.getDecoder().decode(code));
    }
}
