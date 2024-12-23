package com.zhongan.devpilot.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.diagnostic.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class JsonUtils {
    private static final Logger log = Logger.getInstance(JsonUtils.class);

    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    }

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.warn("Error occurred while transform json.", e);
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Error occurred while parsing from json.", e);
            return null;
        }
    }

    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.warn("Error occurred while parsing from json list.", e);
            return null;
        }
    }

    public static boolean toJson(File file, Object object) {
        try {
            objectMapper.writeValue(file, object);
        } catch (IOException e) {
            log.warn("Error occurred while writing into file.", e);
            return false;
        }

        return true;
    }

    public static <T> T fromJson(File file, Class<T> clazz) {
        try {
            return objectMapper.readValue(file, clazz);
        } catch (Exception e) {
            log.warn("Error occurred while parsing from json file.", e);
            return null;
        }
    }

    public static String fixJson(String json) {
        String jsonWithoutMarkdown = MarkdownUtil.extractContents(json);

        if (jsonWithoutMarkdown != null) {
            return jsonWithoutMarkdown;
        }

        int index = json.indexOf("{");
        if (index != 0) {
            json = json.substring(index);
        }
        return json;
    }
}
