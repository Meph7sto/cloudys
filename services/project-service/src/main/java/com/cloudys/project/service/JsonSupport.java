package com.cloudys.project.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSupport() {
    }

    static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 序列化失败", e);
        }
    }

    static List<String> toStringList(String json) {
        String normalized = normalizeJson(json);
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = MAPPER.readTree(normalized);
            if (node.isTextual()) {
                return toStringList(node.asText());
            }
            if (!node.isArray()) {
                return List.of();
            }
            return MAPPER.convertValue(node, new TypeReference<ArrayList<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    static Map<String, Object> toMap(String json) {
        String normalized = normalizeJson(json);
        if (normalized == null || normalized.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            JsonNode node = MAPPER.readTree(normalized);
            if (node.isTextual()) {
                return toMap(node.asText());
            }
            if (!node.isObject()) {
                return Collections.emptyMap();
            }
            return MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static String normalizeJson(String json) {
        if (json == null) {
            return null;
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        try {
            JsonNode node = MAPPER.readTree(trimmed);
            if (node.isTextual()) {
                return node.asText();
            }
            return trimmed;
        } catch (Exception e) {
            return trimmed;
        }
    }
}
