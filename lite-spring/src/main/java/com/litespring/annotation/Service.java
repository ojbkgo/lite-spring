package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 服务层注解
 * 语义化的@Component，用于标注服务层组件
 * 
 * @author lite-spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // 元注解：Service本身也是Component
public @interface Service {
    
    /**
     * Bean的名称
     */
    String value() default "";
}

