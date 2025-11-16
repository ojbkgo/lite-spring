package com.litespring.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 属性值集合
 * 存储Bean的所有属性值
 * 
 * @author lite-spring
 */
public class PropertyValues {
    
    private final List<PropertyValue> propertyValueList = new ArrayList<>();
    
    /**
     * 添加属性值
     */
    public void addPropertyValue(PropertyValue pv) {
        this.propertyValueList.add(pv);
    }
    
    /**
     * 添加属性值（便捷方法）
     */
    public void addPropertyValue(String name, Object value) {
        addPropertyValue(new PropertyValue(name, value));
    }
    
    /**
     * 获取所有属性值
     */
    public List<PropertyValue> getPropertyValues() {
        return this.propertyValueList;
    }
    
    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return this.propertyValueList.isEmpty();
    }
}

