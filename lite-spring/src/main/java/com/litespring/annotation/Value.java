package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 值注入注解
 * 用于注入配置值
 * 
 * 第四阶段支持：
 * - 字面值：@Value("100")
 * 
 * 后续可扩展：
 * - 占位符：@Value("${server.port}")
 * - SpEL表达式：@Value("#{systemProperties['user.name']}")
 * 
 * @author lite-spring
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    
    /**
     * 要注入的值
     */
    String value();
}

