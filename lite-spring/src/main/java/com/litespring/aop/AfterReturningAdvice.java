package com.litespring.aop;

import java.lang.reflect.Method;

/**
 * 返回后通知接口
 * 在目标方法成功返回后调用
 * 
 * @author lite-spring
 */
public interface AfterReturningAdvice extends Advice {
    
    /**
     * 在目标方法成功返回后调用
     * 
     * @param returnValue 方法返回值
     * @param method 被调用的方法
     * @param args 方法参数
     * @param target 目标对象
     * @throws Throwable 如果通知逻辑抛出异常
     */
    void afterReturning(Object returnValue, Method method, Object[] args, Object target) 
            throws Throwable;
}

