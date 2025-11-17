package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 自动装配注解
 * 可以标注在字段、方法、构造器上
 * 容器会自动注入匹配的Bean
 * 
 * @author lite-spring
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    
    /**
     * 是否必须
     * true: 找不到Bean会抛异常
     * false: 找不到Bean不注入，不报错
     */
    boolean required() default true;
}

