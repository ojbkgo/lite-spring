package com.litespring.core;

public interface BeanDefinitionRegistry {
    // 注册Bean定义
    void registerBeanDefinition(String beanName, BeanDefinition definition);

    // 获取Bean定义
    BeanDefinition getBeanDefinition(String beanName);
    
    // 是否已经包含Bean定义
    boolean containsBeanDefinition(String beanName);
}
