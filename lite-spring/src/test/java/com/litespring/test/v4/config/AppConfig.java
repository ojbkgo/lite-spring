package com.litespring.test.v4.config;

import com.litespring.annotation.ComponentScan;
import com.litespring.annotation.Configuration;

/**
 * 应用配置类
 * 使用@Configuration和@ComponentScan
 * 
 * @author lite-spring
 */
@Configuration
@ComponentScan("com.litespring.test.v4")  // 扫描v4包下的所有组件
public class AppConfig {
    
    // 第四阶段暂不实现@Bean方法
    // 第五阶段会完善
}

