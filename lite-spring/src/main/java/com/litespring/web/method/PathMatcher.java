package com.litespring.web.method;

import java.util.HashMap;
import java.util.Map;

/**
 * 路径匹配器
 * 支持{变量}占位符
 * 
 * @author lite-spring
 */
public class PathMatcher {
    
    /**
     * 匹配路径
     * @param pattern /users/{id}
     * @param path /users/123
     * @return true if matches
     */
    public static boolean match(String pattern, String path) {
        // 去除开头的/
        pattern = trimSlash(pattern);
        path = trimSlash(path);
        
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        
        // 段数必须相同
        if (patternParts.length != pathParts.length) {
            return false;
        }
        
        // 逐段匹配
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            String pathPart = pathParts[i];
            
            // 如果是变量，跳过（匹配任意值）
            if (isVariable(patternPart)) {
                continue;
            }
            
            // 普通段，必须完全匹配
            if (!patternPart.equals(pathPart)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 提取路径变量
     * @param pattern /users/{id}/orders/{orderId}
     * @param path /users/123/orders/456
     * @return {id=123, orderId=456}
     */
    public static Map<String, String> extractUriVariables(String pattern, String path) {
        Map<String, String> variables = new HashMap<>();
        
        pattern = trimSlash(pattern);
        path = trimSlash(path);
        
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            
            if (isVariable(patternPart)) {
                // 提取变量名：{id} → id
                String varName = patternPart.substring(1, patternPart.length() - 1);
                variables.put(varName, pathParts[i]);
            }
        }
        
        return variables;
    }
    
    /**
     * 判断是否是变量
     */
    private static boolean isVariable(String part) {
        return part.startsWith("{") && part.endsWith("}");
    }
    
    /**
     * 去除开头和结尾的/
     */
    private static String trimSlash(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}

