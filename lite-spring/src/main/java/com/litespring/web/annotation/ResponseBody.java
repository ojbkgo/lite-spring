package com.litespring.web.annotation;

import java.lang.annotation.*;

/**
 * 响应体注解
 * 标注的方法返回值会直接写入HTTP响应体（通常是JSON）
 * 
 * @author lite-spring
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseBody {
}

