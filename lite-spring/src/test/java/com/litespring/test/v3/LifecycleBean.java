package com.litespring.test.v3;

import com.litespring.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 完整生命周期测试Bean
 * 实现所有生命周期接口，记录每个回调的调用顺序
 * 
 * @author lite-spring
 */
public class LifecycleBean implements 
        BeanNameAware, 
        BeanFactoryAware,
        InitializingBean, 
        DisposableBean {
    
    private String beanName;
    private BeanFactory beanFactory;
    private String property;
    
    // 记录所有回调的顺序
    private final List<String> callbacks = new ArrayList<>();
    
    public LifecycleBean() {
        callbacks.add("1.构造函数");
        System.out.println("【1】LifecycleBean构造函数被调用");
    }
    
    // ==================== 属性 ====================
    
    public void setProperty(String property) {
        callbacks.add("2.属性注入");
        this.property = property;
        System.out.println("【2】属性注入: property = " + property);
    }
    
    // ==================== Aware接口 ====================
    
    @Override
    public void setBeanName(String name) {
        callbacks.add("3.BeanNameAware");
        this.beanName = name;
        System.out.println("【3】BeanNameAware.setBeanName: " + name);
    }
    
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        callbacks.add("4.BeanFactoryAware");
        this.beanFactory = beanFactory;
        System.out.println("【4】BeanFactoryAware.setBeanFactory");
    }
    
    // ==================== 初始化接口 ====================
    
    @Override
    public void afterPropertiesSet() throws Exception {
        callbacks.add("5.InitializingBean");
        System.out.println("【5】InitializingBean.afterPropertiesSet");
    }
    
    /**
     * 自定义初始化方法（通过init-method配置）
     */
    public void customInit() {
        callbacks.add("6.customInit");
        System.out.println("【6】customInit方法被调用");
    }
    
    // ==================== 销毁接口 ====================
    
    @Override
    public void destroy() throws Exception {
        callbacks.add("7.DisposableBean");
        System.out.println("【7】DisposableBean.destroy");
    }
    
    /**
     * 自定义销毁方法（通过destroy-method配置）
     */
    public void customDestroy() {
        callbacks.add("8.customDestroy");
        System.out.println("【8】customDestroy方法被调用");
    }
    
    // ==================== Getter方法 ====================
    
    public List<String> getCallbacks() {
        return callbacks;
    }
    
    public String getBeanName() {
        return beanName;
    }
    
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }
    
    public String getProperty() {
        return property;
    }
}

