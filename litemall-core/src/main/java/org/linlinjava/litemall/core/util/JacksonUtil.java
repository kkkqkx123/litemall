package org.linlinjava.litemall.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JacksonUtil {

    private static final Log logger = LogFactory.getLog(JacksonUtil.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // 配置ObjectMapper以支持Java 8时间类型
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // 禁用将日期写为时间戳，保持ISO格式
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static String parseString(String body, String field) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null)
                return leaf.asText();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    public static List<String> parseStringList(String body, String field) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            JsonNode leaf = node.get(field);

            if (leaf != null)
                return objectMapper.convertValue(leaf, new TypeReference<List<String>>() {
                });
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static Integer parseInteger(String body, String field) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null)
                return leaf.asInt();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static List<Integer> parseIntegerList(String body, String field) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            JsonNode leaf = node.get(field);

            if (leaf != null)
                return objectMapper.convertValue(leaf, new TypeReference<List<Integer>>() {
                });
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    public static Boolean parseBoolean(String body, String field) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null)
                return leaf.asBoolean();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static Short parseShort(String body, String field) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null) {
                Integer value = leaf.asInt();
                return value.shortValue();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static Byte parseByte(String body, String field) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null) {
                Integer value = leaf.asInt();
                return value.byteValue();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T parseObject(String body, String field, Class<T> clazz) {
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
            node = node.get(field);
            return objectMapper.treeToValue(node, clazz);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static Object toNode(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    public static Map<String, String> toMap(String data) {
        try {
            return objectMapper.readValue(data, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
