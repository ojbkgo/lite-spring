package com.litespring.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 构造器参数
 * 存储Bean的构造器参数列表
 * 
 * @author lite-spring
 */
public class ConstructorArgument {
    
    private final List<ValueHolder> argumentValues = new ArrayList<>();
    
    /**
     * 添加参数值
     */
    public void addArgumentValue(ValueHolder valueHolder) {
        this.argumentValues.add(valueHolder);
    }
    
    /**
     * 添加参数值（便捷方法）
     */
    public void addArgumentValue(Object value) {
        this.argumentValues.add(new ValueHolder(value));
    }
    
    /**
     * 获取所有参数值
     */
    public List<ValueHolder> getArgumentValues() {
        return this.argumentValues;
    }
    
    /**
     * 获取参数个数
     */
    public int getArgumentCount() {
        return this.argumentValues.size();
    }
    
    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return this.argumentValues.isEmpty();
    }
    
    /**
     * 参数值持有者
     */
    public static class ValueHolder {
        
        private final Object value;
        private String type;
        
        public ValueHolder(Object value) {
            this.value = value;
        }
        
        public ValueHolder(Object value, String type) {
            this.value = value;
            this.type = type;
        }
        
        public Object getValue() {
            return this.value;
        }
        
        public String getType() {
            return this.type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
}

