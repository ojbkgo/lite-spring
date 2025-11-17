package com.litespring.annotation;

import java.lang.annotation.*;

/**
 * 组件注解
 * 标注在类上，表示这是一个Spring管理的组件
 * 
 * @author lite-spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {
    
    /**
     * Bean的名称
     * 默认为类名首字母小写
     */
    String value() default "";
}

