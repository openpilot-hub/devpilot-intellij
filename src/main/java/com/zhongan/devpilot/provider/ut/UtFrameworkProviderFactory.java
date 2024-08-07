package com.zhongan.devpilot.provider.ut;

import com.zhongan.devpilot.provider.ut.java.JavaUtFrameworkProvider;

public class UtFrameworkProviderFactory {

    public static UtFrameworkProvider create(String language) {
        switch (language) {
            case "java":
                return new JavaUtFrameworkProvider();
            case "go":
            case "python":
        }
        // todo support other languages test.
        return null;
    }

}
