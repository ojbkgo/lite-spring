package com.litespring.aop;

/**
 * AOP代理接口
 * 用于创建代理对象
 * 
 * @author lite-spring
 */
public interface AopProxy {
    
    /**
     * 创建代理对象
     */
    Object getProxy();
    
    /**
     * 创建代理对象（指定类加载器）
     */
    Object getProxy(ClassLoader classLoader);
}

