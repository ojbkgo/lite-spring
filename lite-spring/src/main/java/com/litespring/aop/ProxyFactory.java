package com.litespring.aop;

/**
 * 代理工厂
 * 用于创建AOP代理对象
 * 自动选择JDK代理或CGLIB代理
 * 
 * @author lite-spring
 */
public class ProxyFactory extends AdvisedSupport {
    
    /**
     * 创建代理对象
     */
    public Object getProxy() {
        return createAopProxy().getProxy();
    }
    
    /**
     * 创建代理对象（指定类加载器）
     */
    public Object getProxy(ClassLoader classLoader) {
        return createAopProxy().getProxy(classLoader);
    }
    
    /**
     * 创建AopProxy
     * 自动选择JDK代理或CGLIB代理
     */
    private AopProxy createAopProxy() {
        // 判断使用JDK代理还是CGLIB代理
        if (shouldUseJdkProxy()) {
            return new JdkDynamicAopProxy(this);
        } else {
            // 第五阶段简化：暂不实现CGLIB
            throw new UnsupportedOperationException(
                "CGLIB代理暂未实现，请让目标类实现接口以使用JDK代理"
            );
            // return new CglibAopProxy(this);
        }
    }
    
    /**
     * 判断是否应该使用JDK代理
     */
    private boolean shouldUseJdkProxy() {
        // 如果强制使用CGLIB
        if (isProxyTargetClass()) {
            return false;
        }
        
        // 如果目标类实现了接口，使用JDK代理
        Class<?>[] interfaces = getTarget().getClass().getInterfaces();
        return interfaces.length > 0;
    }
}

