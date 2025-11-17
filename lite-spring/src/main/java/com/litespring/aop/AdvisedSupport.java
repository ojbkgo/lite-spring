package com.litespring.aop;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * AOP代理配置
 * 持有代理所需的所有信息
 * 
 * @author lite-spring
 */
public class AdvisedSupport {
    
    private Object target;  // 目标对象
    private Class<?> targetClass;  // 目标类
    private List<Advisor> advisors = new ArrayList<>();  // 通知器列表
    private boolean proxyTargetClass = false;  // 是否强制CGLIB代理
    
    /**
     * 添加通知器
     */
    public void addAdvisor(Advisor advisor) {
        this.advisors.add(advisor);
    }
    
    /**
     * 添加通知器（指定位置）
     */
    public void addAdvisor(int pos, Advisor advisor) {
        this.advisors.add(pos, advisor);
    }
    
    /**
     * 获取所有通知器
     */
    public List<Advisor> getAdvisors() {
        return this.advisors;
    }
    
    /**
     * 获取匹配指定方法的拦截器
     */
    public List<Object> getInterceptors(Method method) {
        List<Object> interceptors = new ArrayList<>();
        
        for (Advisor advisor : this.advisors) {
            if (advisor instanceof PointcutAdvisor) {
                PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
                
                // 检查切点是否匹配
                if (pointcutAdvisor.getPointcut().matches(method, this.targetClass)) {
                    interceptors.add(pointcutAdvisor.getAdvice());
                }
            }
        }
        
        return interceptors;
    }
    
    // ==================== Getter和Setter ====================
    
    public Object getTarget() {
        return target;
    }
    
    public void setTarget(Object target) {
        this.target = target;
        if (target != null) {
            this.targetClass = target.getClass();
        }
    }
    
    public Class<?> getTargetClass() {
        return targetClass;
    }
    
    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }
    
    public boolean isProxyTargetClass() {
        return proxyTargetClass;
    }
    
    public void setProxyTargetClass(boolean proxyTargetClass) {
        this.proxyTargetClass = proxyTargetClass;
    }
}

