package com.parcelflow.tracking.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class Json {
    private static final ObjectMapper MAPPER = createMapper();
    private Json() {}
    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return m;
    }
    public static ObjectMapper mapper() { return MAPPER; }
    public static String toJson(Object obj) throws JsonProcessingException { return MAPPER.writeValueAsString(obj); }
    public static <T> T fromJson(String json, Class<T> type) throws JsonProcessingException { return MAPPER.readValue(json, type); }
    public static <T> T fromJson(byte[] json, Class<T> type) throws java.io.IOException { return MAPPER.readValue(json, type); }
}
