package com.litespring.web.annotation;

import java.lang.annotation.*;

/**
 * 路径变量注解
 * 从URL路径中提取变量值
 * 例如：/users/{id} → @PathVariable int id
 * 
 * @author lite-spring
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {
    
    /**
     * 变量名称
     */
    String value() default "";
    
    /**
     * 变量名称（与value相同）
     */
    String name() default "";
    
    /**
     * 是否必须
     */
    boolean required() default true;
}

