package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 配置类注解
 * 标注在类上，表示这是一个配置类
 * 配置类本身也会被注册为Bean
 * 
 * @author lite-spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // 配置类本身也是组件
public @interface Configuration {
    
    /**
     * Bean的名称
     */
    String value() default "";
}

