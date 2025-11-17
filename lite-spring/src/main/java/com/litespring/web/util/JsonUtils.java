package com.litespring.web.util;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 简单的JSON工具类
 * 第六阶段简化实现，只支持基本的对象序列化
 * 后续可以集成Jackson或Gson
 * 
 * @author lite-spring
 */
public class JsonUtils {
    
    /**
     * 将对象转换为JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        // 基本类型和String
        if (obj instanceof String) {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        // Map
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        
        // Collection
        if (obj instanceof Collection) {
            return collectionToJson((Collection<?>) obj);
        }
        
        // 数组
        if (obj.getClass().isArray()) {
            return arrayToJson(obj);
        }
        
        // 普通对象
        return objectToJson(obj);
    }
    
    /**
     * Map转JSON
     */
    private static String mapToJson(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(toJson(entry.getValue()));
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Collection转JSON
     */
    private static String collectionToJson(Collection<?> collection) {
        if (collection.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        
        for (Object item : collection) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJson(item));
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 数组转JSON
     */
    private static String arrayToJson(Object array) {
        Object[] arr = (Object[]) array;
        return collectionToJson(Arrays.asList(arr));
    }
    
    /**
     * 对象转JSON（通过反射）
     */
    private static String objectToJson(Object obj) {
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                
                if (!first) {
                    sb.append(",");
                }
                first = false;
                
                sb.append("\"").append(field.getName()).append("\":");
                sb.append(toJson(value));
            }
            
            sb.append("}");
            return sb.toString();
            
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * 转义JSON字符串
     */
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * JSON转对象（简化实现）
     * 第六阶段暂不完整实现，后续可扩展
     */
    public static <T> T fromJson(String json, Class<T> type) {
        // TODO: 完整的JSON解析
        // 建议后续集成Gson或Jackson
        throw new UnsupportedOperationException("JSON反序列化暂未实现，建议集成Gson");
    }
}

