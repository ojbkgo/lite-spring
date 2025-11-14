package com.litespring.demo.service.impl;

import com.litespring.demo.service.HelloService;

/**
 * Hello服务实现类
 * 
 * 注意：这是目标代码，当前lite-spring还未实现相关注解
 * 将在后续迭代中逐步实现
 * 
 * @author lite-spring
 */
// @Service
public class HelloServiceImpl implements HelloService {
    
    @Override
    public String greet(String name) {
        return "Hello, " + name + "! Welcome to Lite Spring Framework.";
    }
    
    // 生命周期方法示例
    public void init() {
        System.out.println("HelloService初始化完成");
    }
    
    public void destroy() {
        System.out.println("HelloService销毁");
    }
}

