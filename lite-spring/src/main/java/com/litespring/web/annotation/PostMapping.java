package com.litespring.web.annotation;

import java.lang.annotation.*;

/**
 * POST请求映射注解
 * 
 * @author lite-spring
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.POST)
public @interface PostMapping {
    
    String[] value() default {};
    String[] path() default {};
}

