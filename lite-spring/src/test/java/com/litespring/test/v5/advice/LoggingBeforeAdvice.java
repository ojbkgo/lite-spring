package com.litespring.test.v5.advice;

import com.litespring.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志前置通知
 * 记录方法调用信息
 * 
 * @author lite-spring
 */
public class LoggingBeforeAdvice implements MethodBeforeAdvice {
    
    private final List<String> logs = new ArrayList<>();
    
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        String log = "【Before】调用方法: " + method.getName();
        logs.add(log);
        System.out.println(log);
    }
    
    public List<String> getLogs() {
        return logs;
    }
}

