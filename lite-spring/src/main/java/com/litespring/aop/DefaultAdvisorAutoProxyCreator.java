package com.litespring.aop;

import com.litespring.core.BeanFactory;
import com.litespring.core.BeanPostProcessor;
import com.litespring.core.BeansException;
import com.litespring.core.DefaultBeanFactory_v4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 默认的Advisor自动代理创建器
 * 自动为Bean创建AOP代理
 * 
 * 这是将AOP集成到IoC容器的关键组件
 * 在BeanPostProcessor的后置处理中自动创建代理
 * 
 * @author lite-spring
 */
public class DefaultAdvisorAutoProxyCreator implements BeanPostProcessor {
    
    private BeanFactory beanFactory;
    
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) 
            throws BeansException {
        
        // 1. 跳过AOP基础设施Bean（Advice、Pointcut、Advisor本身不需要被代理）
        if (isInfrastructureClass(bean.getClass())) {
            return bean;
        }
        
        // 2. 获取匹配的Advisor
        List<Advisor> advisors = getMatchingAdvisors(bean.getClass());
        
        // 3. 如果有匹配的Advisor，创建代理
        if (!advisors.isEmpty()) {
            return createProxy(bean, advisors);
        }
        
        // 4. 没有匹配的Advisor，返回原Bean
        return bean;
    }
    
    /**
     * 判断是否是AOP基础设施类
     */
    private boolean isInfrastructureClass(Class<?> beanClass) {
        return Advice.class.isAssignableFrom(beanClass) ||
               Pointcut.class.isAssignableFrom(beanClass) ||
               Advisor.class.isAssignableFrom(beanClass);
    }
    
    /**
     * 获取匹配的Advisor
     */
    private List<Advisor> getMatchingAdvisors(Class<?> beanClass) {
        List<Advisor> matchingAdvisors = new ArrayList<>();
        
        // 从容器中获取所有Advisor类型的Bean
        if (!(beanFactory instanceof DefaultBeanFactory_v4)) {
            return matchingAdvisors;
        }
        
        DefaultBeanFactory_v4 factory = (DefaultBeanFactory_v4) beanFactory;
        Map<String, Advisor> advisorBeans = factory.getBeansOfType(Advisor.class);
        
        // 检查每个Advisor是否适用于当前Bean
        for (Advisor advisor : advisorBeans.values()) {
            if (canApply(advisor, beanClass)) {
                matchingAdvisors.add(advisor);
            }
        }
        
        return matchingAdvisors;
    }
    
    /**
     * 判断Advisor是否适用于目标类
     */
    private boolean canApply(Advisor advisor, Class<?> targetClass) {
        if (!(advisor instanceof PointcutAdvisor)) {
            // 如果不是PointcutAdvisor，默认适用
            return true;
        }
        
        PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
        Pointcut pointcut = pointcutAdvisor.getPointcut();
        
        // 检查类中是否有方法匹配切点
        Method[] methods = targetClass.getDeclaredMethods();
        for (Method method : methods) {
            if (pointcut.matches(method, targetClass)) {
                return true;  // 有任意一个方法匹配就适用
            }
        }
        
        return false;
    }
    
    /**
     * 创建代理对象
     */
    private Object createProxy(Object bean, List<Advisor> advisors) {
        ProxyFactory proxyFactory = new ProxyFactory();
        
        // 设置目标对象
        proxyFactory.setTarget(bean);
        proxyFactory.setTargetClass(bean.getClass());
        
        // 添加所有Advisor
        for (Advisor advisor : advisors) {
            proxyFactory.addAdvisor(advisor);
        }
        
        // 创建并返回代理
        return proxyFactory.getProxy();
    }
}

