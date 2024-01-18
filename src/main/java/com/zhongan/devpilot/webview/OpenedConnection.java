package com.zhongan.devpilot.webview;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefResponse;

public class OpenedConnection implements ResourceHandlerState {
    private final URLConnection connection;

    private InputStream inputStream;

    public OpenedConnection(URLConnection connection) {
        this.connection = connection;
        this.inputStream = null;
    }

    private InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = connection.getInputStream();
        }
        return inputStream;
    }

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef responseLength, StringRef redirectUrl) {
        try {
            String url = connection.getURL().toString();
            if (url.contains("css")) {
                cefResponse.setMimeType("text/css");
            } else if (url.contains("js")) {
                cefResponse.setMimeType("text/javascript");
            } else if (url.contains("html")) {
                cefResponse.setMimeType("text/html");
            } else {
                cefResponse.setMimeType(connection.getContentType());
            }
            responseLength.set(getInputStream().available());
            cefResponse.setStatus(200);
        } catch (IOException e) {
            cefResponse.setError(CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND);
            cefResponse.setStatusText(e.getLocalizedMessage());
            cefResponse.setStatus(404);
        }
    }

    @Override
    public boolean readResponse(byte[] dataOut, int designedBytesToRead, IntRef bytesRead, CefCallback callback) {
        try {
            int availableSize = getInputStream().available();
            if (availableSize > 0) {
                int maxBytesToRead = Math.min(availableSize, designedBytesToRead);
                int realNumberOfReadBytes = getInputStream().read(dataOut, 0, maxBytesToRead);
                bytesRead.set(realNumberOfReadBytes);
                return true;
            } else {
                getInputStream().close();
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
