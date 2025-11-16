package com.litespring.core;

/**
 * BeanFactory感知接口
 * 实现此接口的Bean可以获取BeanFactory的引用
 * 
 * @author lite-spring
 */
public interface BeanFactoryAware extends Aware {
    
    /**
     * 设置BeanFactory
     * 在属性注入之后、初始化之前调用
     * 
     * @param beanFactory BeanFactory实例
     * @throws BeansException 设置失败时抛出
     */
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}

