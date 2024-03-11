package com.zhongan.devpilot.rest;

import com.zhongan.devpilot.enums.LoginTypeEnum;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.util.CallbackUtils;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.RestServiceUtils;
import com.zhongan.devpilot.util.WxAuthUtils;
import com.zhongan.devpilot.util.ZaSsoUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

public class LoginAuthCallbackRestService extends RestService {
    @Nullable
    @Override
    public String execute(@NotNull QueryStringDecoder queryStringDecoder, @NotNull FullHttpRequest fullHttpRequest, @NotNull ChannelHandlerContext channelHandlerContext) throws IOException {
        var token = RestServiceUtils.getParameter(queryStringDecoder, "token");
        var scope = RestServiceUtils.getParameter(queryStringDecoder, "scope");

        if (token == null || scope == null) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return "token or scope should not null";
        }

        var user = RestServiceUtils.parseToken(token);

        if (user == null) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return "user analysis fail";
        }

        LoginTypeEnum loginType;
        String username;

        switch (scope) {
            case "gzh":
                loginType = LoginTypeEnum.WX;
                username = user.getNickname();
                WxAuthUtils.login(user.getToken(), user.getNickname(), user.getOpenid());
                break;
            case "za":
                loginType = LoginTypeEnum.ZA;
                username = user.getUsername();
                ZaSsoUtils.login(ZaSsoEnum.ZA, user.getToken(), user.getUsername());
                break;
            case "zati":
                loginType = LoginTypeEnum.ZA_TI;
                username = user.getUsername();
                ZaSsoUtils.login(ZaSsoEnum.ZA_TI, user.getToken(), user.getUsername());
                break;
            default:
                sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
                return "scope invalid";
        }

        var setting = DevPilotLlmSettingsState.getInstance();
        setting.setLoginType(loginType.getType());

        LoginUtils.changeLoginStatus(true);

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
        return "login/auth/callback";
    }
}
