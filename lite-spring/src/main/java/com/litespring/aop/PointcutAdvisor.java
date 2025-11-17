package com.litespring.aop;

/**
 * 切点通知器接口
 * 组合了Pointcut和Advice
 * 
 * @author lite-spring
 */
public interface PointcutAdvisor extends Advisor {
    
    /**
     * 获取切点
     */
    Pointcut getPointcut();
}

