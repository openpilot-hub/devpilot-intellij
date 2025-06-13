package com.zhongan.devpilot.provider.ut;

import com.zhongan.devpilot.provider.ut.java.DefaultJavaUtFrameworkProvider;
import com.zhongan.devpilot.provider.ut.java.JavaUtFrameworkProvider;
import com.zhongan.devpilot.util.LanguageUtil;

import java.util.Locale;

public class UtFrameworkProviderFactory {

    public static UtFrameworkProvider create(LanguageUtil.Language language) {

        if (language == null) {
            return null;
        }

        switch (language.getLanguageName().toLowerCase(Locale.ROOT)) {
            case "java":
                try {
                    Class.forName("org.jetbrains.idea.maven.model.MavenArtifact");
                    return JavaUtFrameworkProvider.INSTANCE;
                } catch (ClassNotFoundException e) {
                    return DefaultJavaUtFrameworkProvider.INSTANCE;
                }
            case "go":
            case "python":
        }
        // todo support other languages test.
        return null;
    }

    public static UtFrameworkProvider create(String language) {

        if (language == null) {
            return null;
        }

        switch (language.toLowerCase(Locale.ROOT)) {
            case "java":
                try {
                    Class.forName("org.jetbrains.idea.maven.model.MavenArtifact");
                    return JavaUtFrameworkProvider.INSTANCE;
                } catch (ClassNotFoundException e) {
                    return DefaultJavaUtFrameworkProvider.INSTANCE;
                }
            case "go":
            case "python":
        }
        // todo support other languages test.
        return null;
    }

}
