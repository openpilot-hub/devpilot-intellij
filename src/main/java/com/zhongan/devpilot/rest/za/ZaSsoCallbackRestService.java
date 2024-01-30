package com.zhongan.devpilot.rest.za;

import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.util.CallbackUtils;
import com.zhongan.devpilot.util.ConfigurableUtils;
import com.zhongan.devpilot.util.RestServiceUtils;
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
        var token = RestServiceUtils.getParameter(queryStringDecoder, "token");
        var scope = RestServiceUtils.getParameter(queryStringDecoder, "scope");

        if (token == null || scope == null) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return null;
        }

        var user = RestServiceUtils.parseToken(token);

        if (user == null) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return null;
        }

        ZaSsoEnum selectedZaSso = scope.equals("zati") ? ZaSsoEnum.ZA_TI : ZaSsoEnum.ZA;
        var configurableCache = ConfigurableUtils.getConfigurableCache();

        if (configurableCache != null) {
            configurableCache.getDevPilotConfigForm().zaSsoLogin(selectedZaSso, user.getToken(), user.getUsername());
        } else {
            ZaSsoUtils.login(selectedZaSso, user.getToken(), user.getUsername());
        }

        var project = getLastFocusedOrOpenedProject();
        if (project != null) {
            var service = project.getService(DevPilotChatToolWindowService.class);
            service.callLoginSuccess(user.getUsername(), selectedZaSso.getDisplayName());
        }

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
