package com.zhongan.devpilot.provider.file;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.zhongan.devpilot.provider.file.java.JavaFileAnalyzeProvider;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileAnalyzeProviderFactory {
    private static final Map<String, FileAnalyzeProvider> providerMap = new ConcurrentHashMap<>();

    private static final DefaultFileAnalyzeProvider defaultProvider = new DefaultFileAnalyzeProvider();

    static {
        providerMap.put("java", new JavaFileAnalyzeProvider());
    }

    public static FileAnalyzeProvider getProvider(String language) {
        if (language == null) {
            return defaultProvider;
        }

        language = language.toLowerCase(Locale.ROOT);
        var provider = providerMap.get(language);
        if (provider == null) {
            return defaultProvider;
        }

        var model = PluginManagerCore.getPlugin(PluginId.getId(provider.moduleName()));
        if (model == null) {
            return defaultProvider;
        }

        return provider;
    }
}
