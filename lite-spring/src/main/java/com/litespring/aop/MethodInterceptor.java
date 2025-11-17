package com.litespring.aop;

/**
 * 方法拦截器接口（环绕通知）
 * 最强大的通知类型，可以完全控制目标方法的执行
 * 
 * @author lite-spring
 */
public interface MethodInterceptor extends Advice {
    
    /**
     * 拦截方法调用
     * 
     * @param invocation 方法调用对象
     * @return 方法返回值
     * @throws Throwable 如果方法执行或通知逻辑抛出异常
     */
    Object invoke(MethodInvocation invocation) throws Throwable;
}

