package com.github.shauway.mal.demo.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by shauway on 2018/1/5.
 */

public class JsonUtil {
    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        DateFormat df = new SimpleDateFormat("");
        objectMapper.setDateFormat(df);
    }

    private JsonUtil() {

    }

    public static JavaType parametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
    }

    public static JavaType parametricType(Class<?> parametrized, JavaType... parameterTypes) {
        return objectMapper.getTypeFactory().constructParametricType(parametrized, parameterTypes);
    }

    public static ObjectMapper getObjectMapper(boolean indent) {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
        objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        return objectMapper;
    }

    public static ObjectMapper getObjectMapper() {
        return getObjectMapper(false);
    }

    public static boolean isValidJson(String json) {
        if (json == null || json.trim().length() == 0) {
            return false;
        }
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
