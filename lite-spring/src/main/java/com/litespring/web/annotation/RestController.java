package com.litespring.web.annotation;

import com.litespring.annotation.Controller;

import java.lang.annotation.*;

/**
 * REST控制器注解
 * 组合了@Controller和@ResponseBody
 * 所有方法默认返回JSON
 * 
 * @author lite-spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface RestController {
    
    /**
     * Bean的名称
     */
    String value() default "";
}

