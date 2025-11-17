package com.litespring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK动态代理实现
 * 使用Java原生的Proxy和InvocationHandler
 * 
 * @author lite-spring
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private final AdvisedSupport advised;
    
    public JdkDynamicAopProxy(AdvisedSupport config) {
        this.advised = config;
    }
    
    @Override
    public Object getProxy() {
        return getProxy(getClass().getClassLoader());
    }
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Class<?>[] interfaces = advised.getTarget().getClass().getInterfaces();
        
        if (interfaces.length == 0) {
            throw new IllegalArgumentException(
                "目标对象没有实现接口，无法使用JDK动态代理。" +
                "请考虑使用CGLIB代理或让目标类实现接口"
            );
        }
        
        return Proxy.newProxyInstance(classLoader, interfaces, this);
    }
    
    /**
     * 代理方法调用
     * 这是JDK动态代理的核心方法
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object target = advised.getTarget();
        
        // 处理Object类的方法（equals、hashCode、toString）
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        
        // 获取匹配的拦截器链
        List<Object> chain = advised.getInterceptors(method);
        
        // 如果没有拦截器，直接调用目标方法
        if (chain.isEmpty()) {
            return method.invoke(target, args);
        }
        
        // 创建方法调用对象，执行拦截器链
        MethodInvocation invocation = new ReflectiveMethodInvocation(
            target, method, args, chain
        );
        
        // 执行拦截器链
        return invocation.proceed();
    }
}

