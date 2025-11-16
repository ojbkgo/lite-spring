package com.litespring.core;

/**
 * Bean后置处理器接口
 * 允许在Bean初始化前后插入自定义逻辑
 * 这是Spring最重要的扩展点之一
 * 
 * @author lite-spring
 */
public interface BeanPostProcessor {
    
    /**
     * 在Bean初始化之前调用
     * 可以对Bean进行包装或修改
     * 
     * @param bean Bean实例
     * @param beanName Bean名称
     * @return 处理后的Bean（可以是原Bean，也可以是包装后的Bean）
     * @throws BeansException 处理失败时抛出
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) 
            throws BeansException {
        return bean;
    }
    
    /**
     * 在Bean初始化之后调用
     * 可以对Bean进行包装或修改（如创建AOP代理）
     * 
     * @param bean Bean实例
     * @param beanName Bean名称
     * @return 处理后的Bean（可以是原Bean，也可以是代理对象）
     * @throws BeansException 处理失败时抛出
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) 
            throws BeansException {
        return bean;
    }
}

