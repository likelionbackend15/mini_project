package com.studybuddy.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;  // 추가
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Jackson ObjectMapper 싱글톤 */
public final class JsonUtil {

    // ✅ LocalDateTime 등 Java-Time 지원 + ISO-8601 형식 사용
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())          // Java-Time 지원
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // ↓ 정의되지 않은 JSON 프로퍼티는 무시
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {}                                   // 인스턴스화 금지

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
