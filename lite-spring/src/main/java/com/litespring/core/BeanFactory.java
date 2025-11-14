package com.litespring.core;

/**
 * Bean工厂接口 - IoC容器的顶层接口
 * 定义了获取Bean的基本方法
 * 
 * @author lite-spring
 */
public interface BeanFactory {
    
    /**
     * 根据Bean名称获取Bean实例
     * 
     * @param name Bean的名称
     * @return Bean实例
     * @throws BeansException 如果Bean不存在或创建失败
     */
    Object getBean(String name) throws BeansException;
    
    /**
     * 根据Bean名称和类型获取Bean实例
     * 
     * @param name Bean的名称
     * @param requiredType 期望的Bean类型
     * @param <T> Bean的类型
     * @return Bean实例
     * @throws BeansException 如果Bean不存在、类型不匹配或创建失败
     */
    <T> T getBean(String name, Class<T> requiredType) throws BeansException;
    
    /**
     * 判断容器中是否包含指定名称的Bean
     * 
     * @param name Bean的名称
     * @return 如果包含返回true，否则返回false
     */
    boolean containsBean(String name);
}

