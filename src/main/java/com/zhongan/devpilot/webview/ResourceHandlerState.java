package com.zhongan.devpilot.webview;

import org.cef.callback.CefCallback;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefResponse;

public interface ResourceHandlerState {
    void getResponseHeaders(CefResponse cefResponse, IntRef responseLength, StringRef redirectUrl);

    boolean readResponse(byte[] dataOut, int designedBytesToRead, IntRef bytesRead, CefCallback callback);

    void close();
}
