package com.litespring.aop;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 反射方法调用实现
 * 实现拦截器链的执行（责任链模式）
 * 
 * @author lite-spring
 */
public class ReflectiveMethodInvocation implements MethodInvocation {
    
    private final Object target;
    private final Method method;
    private final Object[] arguments;
    private final List<Object> interceptorsAndDynamicMethodMatchers;
    
    private int currentInterceptorIndex = -1;
    
    public ReflectiveMethodInvocation(
            Object target,
            Method method,
            Object[] arguments,
            List<Object> interceptorsAndDynamicMethodMatchers) {
        
        this.target = target;
        this.method = method;
        this.arguments = arguments;
        this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
    }
    
    @Override
    public Object proceed() throws Throwable {
        // 所有拦截器都执行完了，调用目标方法
        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
            return invokeJoinpoint();
        }
        
        // 获取下一个拦截器
        Object interceptorOrInterceptionAdvice = 
            this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
        
        // 根据类型分别处理
        if (interceptorOrInterceptionAdvice instanceof MethodInterceptor) {
            // 环绕通知
            MethodInterceptor mi = (MethodInterceptor) interceptorOrInterceptionAdvice;
            return mi.invoke(this);
            
        } else if (interceptorOrInterceptionAdvice instanceof MethodBeforeAdvice) {
            // 前置通知
            MethodBeforeAdvice mba = (MethodBeforeAdvice) interceptorOrInterceptionAdvice;
            mba.before(this.method, this.arguments, this.target);
            return proceed();  // 继续执行链
            
        } else if (interceptorOrInterceptionAdvice instanceof AfterReturningAdvice) {
            // 返回后通知
            Object returnValue = proceed();  // 先执行方法
            AfterReturningAdvice ara = (AfterReturningAdvice) interceptorOrInterceptionAdvice;
            ara.afterReturning(returnValue, this.method, this.arguments, this.target);
            return returnValue;
            
        } else {
            // 未知类型，继续
            return proceed();
        }
    }
    
    /**
     * 调用目标方法
     */
    protected Object invokeJoinpoint() throws Throwable {
        return this.method.invoke(this.target, this.arguments);
    }
    
    @Override
    public Method getMethod() {
        return this.method;
    }
    
    @Override
    public Object[] getArguments() {
        return this.arguments;
    }
    
    @Override
    public Object getThis() {
        return this.target;
    }
}

