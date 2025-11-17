package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 数据访问层注解
 * 语义化的@Component，用于标注数据访问层组件
 * 
 * @author lite-spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // 元注解
public @interface Repository {
    
    /**
     * Bean的名称
     */
    String value() default "";
}

