package com.litespring.util;

import com.litespring.core.BeansException;

/**
 * 简单的类型转换器
 * 将字符串转换为目标类型
 * 
 * @author lite-spring
 */
public class SimpleTypeConverter {
    
    /**
     * 将字符串值转换为目标类型
     * 
     * @param value 字符串值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    public Object convertIfNecessary(String value, Class<?> targetType) {
        if (targetType == null) {
            return value;
        }
        
        // 处理null或空字符串
        if (value == null || value.trim().isEmpty()) {
            return handleNullValue(targetType);
        }
        
        // 自动trim
        value = value.trim();
        
        // String类型直接返回
        if (targetType == String.class) {
            return value;
        }
        
        // 类型转换
        try {
            return doConvert(value, targetType);
        } catch (Exception e) {
            throw new BeansException(
                "类型转换失败: 无法将 '" + value + "' 转换为 " + targetType.getName(),
                e
            );
        }
    }
    
    /**
     * 处理null值
     */
    private Object handleNullValue(Class<?> targetType) {
        // 基本类型返回默认值
        if (targetType.isPrimitive()) {
            return getPrimitiveDefault(targetType);
        }
        // 包装类型返回null
        return null;
    }
    
    /**
     * 获取基本类型的默认值
     */
    private Object getPrimitiveDefault(Class<?> type) {
        if (type == int.class) {
            return 0;
        } else if (type == long.class) {
            return 0L;
        } else if (type == double.class) {
            return 0.0;
        } else if (type == float.class) {
            return 0.0f;
        } else if (type == boolean.class) {
            return false;
        } else if (type == short.class) {
            return (short) 0;
        } else if (type == byte.class) {
            return (byte) 0;
        } else if (type == char.class) {
            return '\u0000';
        }
        return null;
    }
    
    /**
     * 执行类型转换
     */
    private Object doConvert(String value, Class<?> targetType) {
        // int / Integer
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        
        // long / Long
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        
        // double / Double
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        
        // float / Float
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        
        // boolean / Boolean（灵活处理）
        if (targetType == boolean.class || targetType == Boolean.class) {
            return parseBoolean(value);
        }
        
        // short / Short
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        }
        
        // byte / Byte
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value);
        }
        
        // char / Character
        if (targetType == char.class || targetType == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException(
                    "char类型只能是单个字符，但收到: " + value
                );
            }
            return value.charAt(0);
        }
        
        throw new UnsupportedOperationException(
            "不支持的类型转换: " + targetType.getName()
        );
    }
    
    /**
     * 解析boolean值（支持多种格式）
     */
    private boolean parseBoolean(String value) {
        String lower = value.toLowerCase();
        if ("true".equals(lower) || "yes".equals(lower) || "1".equals(lower) || "on".equals(lower)) {
            return true;
        }
        if ("false".equals(lower) || "no".equals(lower) || "0".equals(lower) || "off".equals(lower)) {
            return false;
        }
        throw new IllegalArgumentException(
            "无法将 '" + value + "' 转换为boolean，有效值: true/false, yes/no, 1/0, on/off"
        );
    }
}

