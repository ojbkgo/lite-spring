package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 限定符注解
 * 配合@Autowired使用，指定要注入的Bean名称
 * 
 * @author lite-spring
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Qualifier {
    
    /**
     * Bean的名称
     */
    String value();
}

