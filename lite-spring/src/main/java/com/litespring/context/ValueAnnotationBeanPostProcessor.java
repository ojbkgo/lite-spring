package com.litespring.context;

import com.litespring.annotation.Value;
import com.litespring.core.BeanPostProcessor;
import com.litespring.core.BeansException;
import com.litespring.util.SimpleTypeConverter;

import java.lang.reflect.Field;

/**
 * @Value注解处理器
 * 处理@Value注解的字段注入
 * 
 * @author lite-spring
 */
public class ValueAnnotationBeanPostProcessor implements BeanPostProcessor {
    
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) 
            throws BeansException {
        
        // 处理@Value字段
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                injectValue(bean, field, valueAnnotation);
            }
        }
        
        return bean;
    }
    
    /**
     * 注入值
     */
    private void injectValue(Object bean, Field field, Value valueAnnotation) {
        try {
            String value = valueAnnotation.value();
            
            // 第四阶段：简化处理，只支持字面值
            // 后续可扩展：
            // - ${...} 占位符：从配置文件读取
            // - #{...} SpEL表达式：计算表达式
            
            Object resolvedValue = resolveValue(value, field.getType());
            
            // 设置字段
            field.setAccessible(true);
            field.set(bean, resolvedValue);
            
        } catch (IllegalAccessException e) {
            throw new BeansException("@Value注入失败: " + field.getName(), e);
        }
    }
    
    /**
     * 解析值
     */
    private Object resolveValue(String value, Class<?> targetType) {
        // 第四阶段：只支持字面值
        // TODO: 后续支持${...}占位符和#{...}SpEL表达式
        
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        // 使用类型转换器
        return typeConverter.convertIfNecessary(value, targetType);
    }
}

