package com.zhongan.devpilot.util;

import com.zhongan.devpilot.rest.za.ZaSsoCallbackRestService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.io.InputStream;

public class CallbackUtils {
    public static ByteBuf getRedirectPage(String url) throws IOException {
        InputStream inputStream = ZaSsoCallbackRestService.class.getResourceAsStream(url);

        if (inputStream == null) {
            return Unpooled.EMPTY_BUFFER;
        }

        return Unpooled.wrappedBuffer(inputStream.readAllBytes());
    }

    public static DefaultFullHttpResponse buildSuccessResponse() throws IOException {
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, getRedirectPage("/html/login-success.html"));
        response.headers().set("Content-Type", "text/html");
        return response;
    }

    public static DefaultFullHttpResponse buildFailResponse() throws IOException {
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, getRedirectPage("/html/login-fail.html"));
        response.headers().set("Content-Type", "text/html");
        return response;
    }
}
