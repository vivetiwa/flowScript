package com.adobe.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.Map;

public class MapperUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectReader reader = mapper.readerFor(new TypeReference<Map<String, Object>>() {
    });

    public static JsonNode readTree(String value) {
        try {
            return mapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize node. Exception : ", e);
        }
    }

    public static MappingIterator<Map<String, Object>> readValues(byte[] value) {
        try {
            return reader.readValues(value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize node. Exception : ", e);
        }
    }

    public static String writeValueAsString(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize node. Exception : ", e);
        }
    }
}
