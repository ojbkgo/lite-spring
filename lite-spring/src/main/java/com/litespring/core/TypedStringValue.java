package com.litespring.core;

/**
 * 类型化字符串值
 * 封装需要类型转换的字符串值
 * XML中的value属性值
 * 
 * @author lite-spring
 */
public class TypedStringValue {
    
    private final String value;
    
    public TypedStringValue(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return this.value;
    }
}

