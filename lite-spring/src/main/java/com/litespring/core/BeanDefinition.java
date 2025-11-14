package com.litespring.core;

/**
 * Bean定义 - 存储Bean的元数据信息
 * 包括Bean的类名、作用域、属性等
 * 
 * @author lite-spring
 */
public class BeanDefinition {
    
    /**
     * Bean的完全限定类名
     */
    private String beanClassName;
    
    /**
     * Bean的作用域：singleton（单例）或 prototype（原型）
     * 默认为单例
     */
    private String scope = "singleton";
    
    /**
     * 是否懒加载
     */
    private boolean lazyInit = false;
    
    /**
     * Bean的初始化方法名
     */
    private String initMethodName;
    
    /**
     * Bean的销毁方法名
     */
    private String destroyMethodName;
    
    public BeanDefinition(String beanClassName) {
        this.beanClassName = beanClassName;
    }
    
    public String getBeanClassName() {
        return beanClassName;
    }
    
    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public boolean isSingleton() {
        return "singleton".equals(scope);
    }
    
    public boolean isPrototype() {
        return "prototype".equals(scope);
    }
    
    public boolean isLazyInit() {
        return lazyInit;
    }
    
    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }
    
    public String getInitMethodName() {
        return initMethodName;
    }
    
    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }
    
    public String getDestroyMethodName() {
        return destroyMethodName;
    }
    
    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }
}

