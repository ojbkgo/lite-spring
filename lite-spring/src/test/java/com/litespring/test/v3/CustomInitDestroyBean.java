package com.litespring.test.v3;

/**
 * 使用自定义init-method和destroy-method的Bean
 * 
 * @author lite-spring
 */
public class CustomInitDestroyBean {
    
    private boolean initialized = false;
    private boolean destroyed = false;
    
    public CustomInitDestroyBean() {
        System.out.println("CustomInitDestroyBean构造函数");
    }
    
    /**
     * 自定义初始化方法
     */
    public void myInit() {
        this.initialized = true;
        System.out.println("customInit方法被调用");
    }
    
    /**
     * 自定义销毁方法
     */
    public void myDestroy() {
        this.destroyed = true;
        System.out.println("customDestroy方法被调用");
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
}

