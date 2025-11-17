package com.litespring.context;

import com.litespring.annotation.Autowired;
import com.litespring.annotation.Qualifier;
import com.litespring.core.BeanFactory;
import com.litespring.core.BeanPostProcessor;
import com.litespring.core.BeansException;
import com.litespring.core.DefaultBeanFactory_v4;

import java.lang.reflect.Field;


/**
 * @Autowired注解处理器
 * 实现自动装配功能
 * 
 * @author lite-spring
 */
public class AutowiredAnnotationBeanPostProcessor implements BeanPostProcessor {
    
    private BeanFactory beanFactory;
    
    public AutowiredAnnotationBeanPostProcessor() {
    }
    
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) 
            throws BeansException {
        
        // 处理@Autowired字段
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                injectField(bean, field, autowired);
            }
        }
        
        return bean;
    }
    
    /**
     * 注入字段
     */
    private void injectField(Object bean, Field field, Autowired autowired) {
        try {
            // 1. 获取要注入的值
            Object value = getAutowiredValue(field, autowired.required());
            
            if (value != null) {
                // 2. 设置字段可访问
                field.setAccessible(true);
                
                // 3. 注入值
                field.set(bean, value);
            } else if (autowired.required()) {
                throw new BeansException(
                    "无法自动装配字段: " + field.getName() + 
                    ", 类型: " + field.getType().getName()
                );
            }
            
        } catch (IllegalAccessException e) {
            throw new BeansException("字段注入失败: " + field.getName(), e);
        }
    }
    
    /**
     * 获取要自动装配的值
     */
    private Object getAutowiredValue(Field field, boolean required) {
        // 1. 检查是否有@Qualifier注解
        Qualifier qualifier = field.getAnnotation(Qualifier.class);
        
        if (qualifier != null) {
            // 按名称获取Bean
            String beanName = qualifier.value();
            return beanFactory.getBean(beanName, field.getType());
        }
        
        // 2. 按类型获取Bean
        return getBeanByType(field.getType(), required);
    }
    
    /**
     * 按类型获取Bean
     */
    private Object getBeanByType(Class<?> type, boolean required) {
        // 需要使用扩展的BeanFactory方法
        if (beanFactory instanceof DefaultBeanFactory_v4) {
            DefaultBeanFactory_v4 factory = (DefaultBeanFactory_v4) beanFactory;
            
            try {
                return factory.getBean(type);
            } catch (BeansException e) {
                if (required) {
                    throw e;
                }
                return null;
            }
        }
        
        throw new BeansException("BeanFactory不支持按类型获取Bean");
    }
}

