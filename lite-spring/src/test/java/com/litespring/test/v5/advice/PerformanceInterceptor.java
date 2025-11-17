package com.litespring.test.v5.advice;

import com.litespring.aop.MethodInterceptor;
import com.litespring.aop.MethodInvocation;

/**
 * 性能监控拦截器（环绕通知）
 * 
 * @author lite-spring
 */
public class PerformanceInterceptor implements MethodInterceptor {
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        
        System.out.println("【性能监控】方法开始: " + methodName);
        long start = System.currentTimeMillis();
        
        try {
            // 执行目标方法
            Object result = invocation.proceed();
            
            long end = System.currentTimeMillis();
            long duration = end - start;
            
            System.out.println("【性能监控】方法结束: " + methodName + ", 耗时: " + duration + "ms");
            
            return result;
            
        } catch (Throwable ex) {
            System.out.println("【性能监控】方法异常: " + methodName);
            throw ex;
        }
    }
}

