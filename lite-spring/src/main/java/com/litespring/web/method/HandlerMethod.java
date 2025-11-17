package com.litespring.web.method;

import com.litespring.web.annotation.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 处理器方法
 * 封装Controller Bean和Method
 * 
 * @author lite-spring
 */
public class HandlerMethod {
    
    private final Object bean;          // Controller实例
    private final Method method;        // Controller方法
    private final Class<?> beanType;    // Controller类型
    
    public HandlerMethod(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        this.beanType = bean.getClass();
    }
    
    /**
     * 调用Handler方法
     */
    public Object invoke(Object... args) throws Exception {
        return this.method.invoke(this.bean, args);
    }
    
    /**
     * 获取方法参数类型
     */
    public Class<?>[] getParameterTypes() {
        return this.method.getParameterTypes();
    }
    
    /**
     * 获取方法参数注解
     */
    public Annotation[][] getParameterAnnotations() {
        return this.method.getParameterAnnotations();
    }
    
    /**
     * 获取方法返回类型
     */
    public Class<?> getReturnType() {
        return this.method.getReturnType();
    }
    
    /**
     * 判断方法是否有指定注解
     */
    public boolean hasMethodAnnotation(Class<? extends Annotation> annotationType) {
        return this.method.isAnnotationPresent(annotationType);
    }
    
    /**
     * 判断类或方法是否有@ResponseBody
     */
    public boolean isResponseBody() {
        // 检查方法级别
        if (method.isAnnotationPresent(ResponseBody.class)) {
            return true;
        }
        
        // 检查类级别
        return beanType.isAnnotationPresent(ResponseBody.class);
    }
    
    // Getter方法
    public Object getBean() {
        return bean;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public Class<?> getBeanType() {
        return beanType;
    }
    
    public String getMethodName() {
        return method.getName();
    }
}

