package com.litespring.aop;

import java.lang.reflect.Method;

/**
 * 方法调用接口
 * 代表一个方法的调用，支持拦截器链
 * 
 * @author lite-spring
 */
public interface MethodInvocation {
    
    /**
     * 继续执行拦截器链或目标方法
     * 
     * @return 方法返回值
     * @throws Throwable 如果方法执行失败
     */
    Object proceed() throws Throwable;
    
    /**
     * 获取被调用的方法
     */
    Method getMethod();
    
    /**
     * 获取方法参数
     */
    Object[] getArguments();
    
    /**
     * 获取目标对象
     */
    Object getThis();
}

