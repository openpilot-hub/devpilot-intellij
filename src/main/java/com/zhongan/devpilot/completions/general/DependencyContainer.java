//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.general;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.zhongan.devpilot.completions.inline.InlineCompletionHandler;
import com.zhongan.devpilot.completions.prediction.CompletionFacade;
import org.jetbrains.annotations.NotNull;

public class DependencyContainer {
    private static InlineCompletionHandler inlineCompletionHandler = null;
    private static Gson gson = instanceOfGson();

    public DependencyContainer() {
    }

    public static synchronized Gson instanceOfGson() {
        if (gson == null) {
            gson = (new GsonBuilder()).registerTypeAdapter(Double.class, doubleOrIntSerializer()).create();
        }

        return gson;
    }

    public static InlineCompletionHandler singletonOfInlineCompletionHandler() {
        if (inlineCompletionHandler == null) {
            inlineCompletionHandler = new InlineCompletionHandler(instanceOfCompletionFacade());
        }

        return inlineCompletionHandler;
    }

    public static @NotNull CompletionFacade instanceOfCompletionFacade() {
        return new CompletionFacade();
    }

    private static @NotNull JsonSerializer<Double> doubleOrIntSerializer() {
        return (src, type, jsonSerializationContext) -> src == src.longValue() ? new JsonPrimitive(src.longValue()) : new JsonPrimitive(String.valueOf(src));
    }
}
