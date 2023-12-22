package com.zhongan.devpilot.rest.za;

import com.zhongan.devpilot.util.CallbackUtils;
import com.zhongan.devpilot.util.ConfigurableUtils;
import com.zhongan.devpilot.util.ZaSsoUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

public class ZaSsoCallbackRestService extends RestService {
    @Nullable
    @Override
    public String execute(@NotNull QueryStringDecoder queryStringDecoder, @NotNull FullHttpRequest fullHttpRequest, @NotNull ChannelHandlerContext channelHandlerContext) throws IOException {
        var ticketList = queryStringDecoder.parameters().get("ticket");

        if (ticketList == null || ticketList.isEmpty()) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return null;
        }

        var configurableCache = ConfigurableUtils.getConfigurableCache();
        if (configurableCache == null) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return null;
        }

        var ticket = ticketList.get(0);

        var user = ZaSsoUtils.zaSsoAuth(configurableCache.getDevPilotConfigForm().getSelectedSso(), ticket);

        if (user == null) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return null;
        }

        configurableCache.getDevPilotConfigForm().zaSsoLogin(user.getToken(), user.getUsername());
        sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildSuccessResponse());
        return null;
    }

    @Override
    protected boolean isPrefixlessAllowed() {
        return true;
    }

    @NotNull
    @Override
    protected OriginCheckResult isOriginAllowed(@NotNull HttpRequest request) {
        return OriginCheckResult.ALLOW;
    }

    @NotNull
    @Override
    protected String getServiceName() {
        return "za/sso/callback";
    }
}
