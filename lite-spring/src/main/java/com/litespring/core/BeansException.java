package com.litespring.core;

/**
 * Bean操作相关的异常基类
 * 
 * @author lite-spring
 */
public class BeansException extends RuntimeException {
    
    public BeansException(String message) {
        super(message);
    }
    
    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}

