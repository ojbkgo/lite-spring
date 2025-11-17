package com.litespring.aop;

import java.lang.reflect.Method;

/**
 * 切点接口
 * 用于判断方法是否匹配
 * 
 * @author lite-spring
 */
public interface Pointcut {
    
    /**
     * 判断方法是否匹配切点
     * 
     * @param method 方法
     * @param targetClass 目标类
     * @return 如果匹配返回true
     */
    boolean matches(Method method, Class<?> targetClass);
}

