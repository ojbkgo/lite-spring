package com.litespring.aop;

/**
 * 通知器接口
 * 持有Advice的基础接口
 * 
 * @author lite-spring
 */
public interface Advisor {
    
    /**
     * 获取通知
     */
    Advice getAdvice();
}

