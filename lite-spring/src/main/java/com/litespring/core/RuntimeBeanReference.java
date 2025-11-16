package com.litespring.core;

/**
 * Bean引用
 * 表示对另一个Bean的引用
 * 用于区分Bean引用和普通字符串值
 * 
 * @author lite-spring
 */
public class RuntimeBeanReference {
    
    private final String beanName;
    
    public RuntimeBeanReference(String beanName) {
        this.beanName = beanName;
    }
    
    public String getBeanName() {
        return this.beanName;
    }
}

