package com.litespring.demo.config;

import com.litespring.annotation.ComponentScan;
import com.litespring.annotation.Configuration;

/**
 * Demo应用配置类
 * 使用注解配置代替XML
 * 
 * @author lite-spring
 */
@Configuration
@ComponentScan("com.litespring.demo")
public class DemoConfig {
    
    // 使用@ComponentScan自动扫描com.litespring.demo包下的所有组件
    // 不再需要手动配置每个Bean
    
    // 后续可以在这里定义@Bean方法
}

