package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 控制层注解
 * 语义化的@Component，用于标注控制层组件
 * 
 * @author lite-spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // 元注解
public @interface Controller {
    
    /**
     * Bean的名称
     */
    String value() default "";
}

