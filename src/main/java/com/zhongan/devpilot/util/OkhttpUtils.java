package com.zhongan.devpilot.util;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class OkhttpUtils {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public static OkHttpClient getClient() {
        return client;
    }
}
