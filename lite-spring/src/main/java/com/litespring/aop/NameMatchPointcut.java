package com.litespring.aop;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * 方法名匹配的切点
 * 简化版切点，通过方法名匹配
 * 
 * @author lite-spring
 */
public class NameMatchPointcut implements Pointcut {
    
    private final Set<String> methodNames = new HashSet<>();
    
    /**
     * 添加要匹配的方法名
     */
    public void addMethodName(String methodName) {
        this.methodNames.add(methodName);
    }
    
    /**
     * 设置要匹配的方法名
     */
    public void setMethodNames(String... methodNames) {
        for (String methodName : methodNames) {
            this.methodNames.add(methodName);
        }
    }
    
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return methodNames.contains(method.getName());
    }
}

