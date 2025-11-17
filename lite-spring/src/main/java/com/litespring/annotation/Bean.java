package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * Bean定义注解
 * 标注在@Configuration类的方法上
 * 方法的返回值会被注册为Bean
 * 
 * @author lite-spring
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    
    /**
     * Bean的名称
     * 默认为方法名
     */
    String name() default "";
    
    /**
     * 初始化方法
     */
    String initMethod() default "";
    
    /**
     * 销毁方法
     */
    String destroyMethod() default "";
}

