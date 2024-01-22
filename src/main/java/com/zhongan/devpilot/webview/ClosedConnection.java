package com.zhongan.devpilot.webview;

import org.cef.callback.CefCallback;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefResponse;

public class ClosedConnection implements ResourceHandlerState {
    private static final ClosedConnection instance = new ClosedConnection();

    private ClosedConnection() {}

    public static ClosedConnection getInstance() {
        return instance;
    }

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef responseLength, StringRef redirectUrl) {
        cefResponse.setStatus(404);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int designedBytesToRead, IntRef bytesRead, CefCallback callback) {
        return false;
    }

    @Override
    public void close() {

    }
}
