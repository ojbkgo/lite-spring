package com.litespring.test.v3;

import com.litespring.core.BeanPostProcessor;
import com.litespring.core.BeansException;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用的BeanPostProcessor
 * 记录所有处理过的Bean
 * 
 * @author lite-spring
 */
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    
    private final List<String> beforeInitBeans = new ArrayList<>();
    private final List<String> afterInitBeans = new ArrayList<>();
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) 
            throws BeansException {
        beforeInitBeans.add(beanName);
        System.out.println("【BeanPostProcessor-前置】处理Bean: " + beanName);
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) 
            throws BeansException {
        afterInitBeans.add(beanName);
        System.out.println("【BeanPostProcessor-后置】处理Bean: " + beanName);
        return bean;
    }
    
    public List<String> getBeforeInitBeans() {
        return beforeInitBeans;
    }
    
    public List<String> getAfterInitBeans() {
        return afterInitBeans;
    }
}

