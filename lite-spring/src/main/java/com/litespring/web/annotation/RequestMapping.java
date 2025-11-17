package com.litespring.web.annotation;

import java.lang.annotation.*;

/**
 * 请求映射注解
 * 可以标注在类或方法上
 * 
 * @author lite-spring
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    
    /**
     * URL路径
     */
    String[] value() default {};
    
    /**
     * URL路径（与value相同）
     */
    String[] path() default {};
    
    /**
     * HTTP方法
     */
    RequestMethod[] method() default {};
}

