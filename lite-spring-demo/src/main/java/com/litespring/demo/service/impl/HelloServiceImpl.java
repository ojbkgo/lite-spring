package com.litespring.demo.service.impl;

import com.litespring.annotation.Service;
import com.litespring.demo.service.HelloService;

/**
 * Hello服务实现类
 * 使用@Service注解
 * 
 * @author lite-spring
 */
@Service("helloService")  // 使用注解代替XML配置
public class HelloServiceImpl implements HelloService {
    
    @Override
    public String greet(String name) {
        return "Hello, " + name + "! Welcome to Lite Spring Framework (Annotation-Driven).";
    }
    
    // 生命周期方法
    public void init() {
        System.out.println("HelloService初始化完成（注解驱动）");
    }
    
    public void destroy() {
        System.out.println("HelloService销毁（注解驱动）");
    }
}
