package com.litespring.test.v1.service;

/**
 * 演示Bean生命周期的服务类
 * 展示初始化和销毁方法的使用
 * 
 * @author lite-spring
 */
public class LifecycleService {
    
    private String resourceName;
    private boolean initialized = false;
    private boolean destroyed = false;
    
    public LifecycleService() {
        System.out.println("1. 构造函数被调用");
    }
    
    /**
     * 初始化方法
     * 在Bean的属性设置完成后调用
     * 用于执行初始化逻辑
     */
    public void init() {
        System.out.println("2. init() 方法被调用 - 执行初始化逻辑");
        this.initialized = true;
        
        // 模拟初始化操作
        // 例如：建立数据库连接、打开文件、启动线程池等
        System.out.println("   - 建立连接...");
        System.out.println("   - 加载配置...");
        System.out.println("   - 初始化完成！");
    }
    
    /**
     * 销毁方法
     * 在容器关闭时调用
     * 用于清理资源
     */
    public void destroy() {
        System.out.println("4. destroy() 方法被调用 - 执行清理逻辑");
        this.destroyed = true;
        
        // 模拟清理操作
        // 例如：关闭数据库连接、关闭文件、停止线程池等
        System.out.println("   - 关闭连接...");
        System.out.println("   - 释放资源...");
        System.out.println("   - 清理完成！");
    }
    
    /**
     * 业务方法
     */
    public void doWork() {
        if (!initialized) {
            throw new IllegalStateException("Service未初始化！");
        }
        if (destroyed) {
            throw new IllegalStateException("Service已销毁！");
        }
        
        System.out.println("3. 执行业务逻辑...");
    }
    
    // Getter和Setter
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public String getResourceName() {
        return resourceName;
    }
    
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}

