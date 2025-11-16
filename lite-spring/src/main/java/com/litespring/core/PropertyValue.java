package com.litespring.core;

/**
 * 属性值封装
 * 封装单个属性的名称和值
 * 
 * @author lite-spring
 */
public class PropertyValue {
    
    private final String name;
    private final Object value;
    
    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }
    
    public String getName() {
        return name;
    }
    
    public Object getValue() {
        return value;
    }
}

