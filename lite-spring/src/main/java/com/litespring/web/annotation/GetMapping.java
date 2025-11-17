package com.litespring.web.annotation;

import java.lang.annotation.*;

/**
 * GET请求映射注解
 * 等价于 @RequestMapping(method = RequestMethod.GET)
 * 
 * @author lite-spring
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.GET)
public @interface GetMapping {
    
    /**
     * URL路径
     */
    String[] value() default {};
    
    /**
     * URL路径（与value相同）
     */
    String[] path() default {};
}

