package com.zhongan.devpilot.exception;

import com.intellij.diagnostic.ITNReporter;
import com.intellij.diagnostic.PluginException;
import com.intellij.ide.plugins.PluginUtil;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.Consumer;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import java.awt.Component;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * see {@link com.intellij.diagnostic.DefaultIdeaErrorLogger}
 */
public class DevPilotErrorReporter extends ITNReporter {

    private static final String DEPRECATED_DEFAULT_PREFIX = "The default implementation of method";

    private static final String DEPRECATED_DEFAULT_SUFFIX = "is deprecated, you need to override it in";

    private static final String DEPRECATED_USAGE = "is deprecated and going to be removed soon.";

    private final PluginId devpilotPluginId = PluginId.getId("com.zhongan.devPilot");


    /**
     * 忽略废弃方法的使用异常提醒，在开发插件过程中(internal model), 还是可以收到相应的提示或警告,提醒开发人员关注和修改
     * @param event
     * @return
     */
    @Override
    public boolean showErrorInRelease(IdeaLoggingEvent event) {
        boolean isDevpilotDeprecatedUseNotice = false;
        Throwable t = event.getThrowable();
        PluginId pluginId = PluginUtil.getInstance().findPluginId(t);
        if (Objects.equals(pluginId, devpilotPluginId)) {
            if (t instanceof PluginException) {
                PluginException pluginException = (PluginException) t;
                String message = pluginException.getMessage();
                if (StringUtils.isNoneBlank(message)) {
                    if (message.contains(DEPRECATED_USAGE) || (message.contains(DEPRECATED_DEFAULT_PREFIX) && message.contains(DEPRECATED_DEFAULT_SUFFIX))) {
                        isDevpilotDeprecatedUseNotice = true;
                    }
                }
            }
        }
        return !isDevpilotDeprecatedUseNotice;
    }

    @NotNull
    @Override
    public String getReportActionText() {
        return DevPilotMessageBundle.get("devpilot.error.report");
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        // do nothing
        return true;
    }

}
