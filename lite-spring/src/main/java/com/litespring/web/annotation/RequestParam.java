package com.litespring.web.annotation;

import java.lang.annotation.*;

/**
 * 请求参数注解
 * 从query参数或form参数获取值
 * 
 * @author lite-spring
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    
    /**
     * 参数名称
     */
    String value() default "";
    
    /**
     * 参数名称（与value相同）
     */
    String name() default "";
    
    /**
     * 是否必须
     */
    boolean required() default true;
    
    /**
     * 默认值
     */
    String defaultValue() default "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";
}

