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

    public static synchronized Gson instanceOfGson() {
        if (gson == null) {
            gson = new GsonBuilder().registerTypeAdapter(Double.class, doubleOrIntSerializer()).create();
        }

        return gson;
    }

    public static InlineCompletionHandler singletonOfInlineCompletionHandler() {
        if (inlineCompletionHandler == null) {
            inlineCompletionHandler =
                    new InlineCompletionHandler(
                            instanceOfCompletionFacade()
                    );
        }

        return inlineCompletionHandler;
    }

    @NotNull
    public static CompletionFacade instanceOfCompletionFacade() {
        return new CompletionFacade();
    }

    @NotNull
    private static JsonSerializer<Double> doubleOrIntSerializer() {
        return (src, type, jsonSerializationContext) -> {
            if (src == src.longValue()) {
                return new JsonPrimitive(src.longValue());
            }
            return new JsonPrimitive(src);
        };
    }
}
