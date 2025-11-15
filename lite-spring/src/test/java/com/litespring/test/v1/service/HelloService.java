package com.litespring.test.v1.service;

/**
 * 测试用的简单服务类
 * 用于验证Bean的创建和获取功能
 * 
 * @author lite-spring
 */
public class HelloService {
    
    /**
     * 无参构造函数
     * IoC容器会通过反射调用这个构造函数创建实例
     */
    public HelloService() {
        System.out.println("HelloService实例被创建");
    }
    
    /**
     * 简单的业务方法
     */
    public String sayHello() {
        return "Hello, Lite Spring!";
    }
    
    /**
     * 带参数的业务方法
     */
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}

