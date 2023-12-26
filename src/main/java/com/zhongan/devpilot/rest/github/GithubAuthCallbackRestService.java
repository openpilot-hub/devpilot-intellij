package com.zhongan.devpilot.rest.github;

import com.zhongan.devpilot.gui.toolwindows.DevPilotChatToolWindowFactory;
import com.zhongan.devpilot.util.CallbackUtils;
import com.zhongan.devpilot.util.ConfigurableUtils;
import com.zhongan.devpilot.util.GithubAuthUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

public class GithubAuthCallbackRestService extends RestService {
    @Nullable
    @Override
    public String execute(@NotNull QueryStringDecoder queryStringDecoder, @NotNull FullHttpRequest fullHttpRequest, @NotNull ChannelHandlerContext channelHandlerContext) throws IOException {
        var codeList = queryStringDecoder.parameters().get("code");

        if (codeList == null || codeList.isEmpty()) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return null;
        }

        var code = codeList.get(0);
        var user = GithubAuthUtils.githubAuth(code);

        if (user == null) {
            sendResponse(fullHttpRequest, channelHandlerContext, CallbackUtils.buildFailResponse());
            return null;
        }

        var configurableCache = ConfigurableUtils.getConfigurableCache();
        if (configurableCache != null) {
            configurableCache.getDevPilotConfigForm().githubLogin(user.getUsername(), user.getToken(), user.getId());
        } else {
            GithubAuthUtils.login(user);
        }

        var project = getLastFocusedOrOpenedProject();

        if (project != null) {
            var toolWindow = DevPilotChatToolWindowFactory.getDevPilotChatToolWindow(project);
            if (toolWindow != null) {
                toolWindow.githubLoginSuccess(user.getUsername());
            }
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
        return "github/callback";
    }
}
