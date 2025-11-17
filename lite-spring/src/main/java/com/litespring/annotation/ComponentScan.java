package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 组件扫描注解
 * 指定要扫描的包路径
 * 
 * @author lite-spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ComponentScan {
    
    /**
     * 要扫描的包路径
     */
    String[] value() default {};
    
    /**
     * 要扫描的包路径（与value相同，提供语义化选择）
     */
    String[] basePackages() default {};
}

