package com.litespring.core;

/**
 * Bean名称感知接口
 * 实现此接口的Bean可以获取自己在容器中的名称
 * 
 * @author lite-spring
 */
public interface BeanNameAware extends Aware {
    
    /**
     * 设置Bean的名称
     * 在属性注入之后、初始化之前调用
     * 
     * @param name Bean的名称
     */
    void setBeanName(String name);
}

