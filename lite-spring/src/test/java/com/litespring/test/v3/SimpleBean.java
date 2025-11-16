package com.litespring.test.v3;

import com.litespring.core.InitializingBean;

/**
 * 简单的InitializingBean实现
 * 
 * @author lite-spring
 */
public class SimpleBean implements InitializingBean {
    
    private boolean initialized = false;
    private String name;
    
    public SimpleBean() {
        System.out.println("SimpleBean构造函数");
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        this.initialized = true;
        System.out.println("SimpleBean初始化完成");
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}

