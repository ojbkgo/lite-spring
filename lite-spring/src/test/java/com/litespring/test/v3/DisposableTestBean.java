package com.litespring.test.v3;

import com.litespring.core.DisposableBean;
import com.litespring.core.InitializingBean;

/**
 * 测试销毁方法的Bean
 * 
 * @author lite-spring
 */
public class DisposableTestBean implements InitializingBean, DisposableBean {
    
    private boolean initialized = false;
    private boolean destroyed = false;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        this.initialized = true;
        System.out.println("DisposableTestBean初始化");
    }
    
    @Override
    public void destroy() throws Exception {
        this.destroyed = true;
        System.out.println("DisposableTestBean销毁");
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
}

