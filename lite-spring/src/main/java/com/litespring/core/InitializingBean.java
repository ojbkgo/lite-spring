package com.litespring.core;

/**
 * 初始化Bean接口
 * 实现此接口的Bean在属性设置完成后会调用afterPropertiesSet方法
 * 
 * @author lite-spring
 */
public interface InitializingBean {
    
    /**
     * 在Bean的属性设置完成后调用
     * 用于执行初始化逻辑
     * 
     * @throws Exception 初始化过程中的异常
     */
    void afterPropertiesSet() throws Exception;
}

