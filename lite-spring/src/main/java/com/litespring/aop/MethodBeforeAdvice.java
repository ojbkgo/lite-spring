package com.litespring.aop;

import java.lang.reflect.Method;

/**
 * 前置通知接口
 * 在目标方法执行前调用
 * 
 * @author lite-spring
 */
public interface MethodBeforeAdvice extends Advice {
    
    /**
     * 在目标方法执行前调用
     * 
     * @param method 被调用的方法
     * @param args 方法参数
     * @param target 目标对象
     * @throws Throwable 如果通知逻辑抛出异常
     */
    void before(Method method, Object[] args, Object target) throws Throwable;
}

