package com.studybuddy.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson ObjectMapper 싱글톤 제공.
 */
public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
