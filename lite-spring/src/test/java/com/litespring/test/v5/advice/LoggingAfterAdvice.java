package com.litespring.test.v5.advice;

import com.litespring.aop.AfterReturningAdvice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志返回后通知
 * 
 * @author lite-spring
 */
public class LoggingAfterAdvice implements AfterReturningAdvice {
    
    private final List<String> logs = new ArrayList<>();
    
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) 
            throws Throwable {
        String log = "【After】方法返回: " + method.getName() + ", 返回值: " + returnValue;
        logs.add(log);
        System.out.println(log);
    }
    
    public List<String> getLogs() {
        return logs;
    }
}

