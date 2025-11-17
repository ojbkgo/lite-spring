package com.litespring.web.annotation;

import java.lang.annotation.*;

/**
 * 请求体注解
 * 从HTTP请求体解析对象（通常是JSON）
 * 
 * @author lite-spring
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
    
    /**
     * 是否必须
     */
    boolean required() default true;
}

