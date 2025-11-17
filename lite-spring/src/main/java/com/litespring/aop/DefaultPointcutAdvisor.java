package com.litespring.aop;

/**
 * 默认的切点通知器实现
 * 组合Pointcut和Advice
 * 
 * @author lite-spring
 */
public class DefaultPointcutAdvisor implements PointcutAdvisor {
    
    private Pointcut pointcut;
    private Advice advice;
    
    public DefaultPointcutAdvisor() {
    }
    
    public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
        this.pointcut = pointcut;
        this.advice = advice;
    }
    
    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
    
    public void setPointcut(Pointcut pointcut) {
        this.pointcut = pointcut;
    }
    
    @Override
    public Advice getAdvice() {
        return advice;
    }
    
    public void setAdvice(Advice advice) {
        this.advice = advice;
    }
}

