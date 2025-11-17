package com.litespring.demo;

import com.litespring.context.AnnotationConfigApplicationContext;
import com.litespring.demo.config.DemoConfig;
import com.litespring.demo.service.HelloService;

/**
 * Demo应用启动类
 * 
 * 阶段1-3：使用XML配置
 * 阶段4：使用注解配置（当前）
 * 阶段9：Spring Boot风格启动（未来）
 * 
 * @author lite-spring
 */
public class DemoApplication {
    
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("Lite Spring Demo Application");
        System.out.println("使用注解驱动配置");
        System.out.println("=================================\n");
        
        // 第四阶段：使用注解配置
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(DemoConfig.class);
        
        // 获取Bean（按类型）
        HelloService helloService = ctx.getBean(HelloService.class);
        
        // 使用Bean
        String result = helloService.greet("Lite Spring");
        System.out.println(result);
        
        // 关闭容器
        System.out.println("\n=================================");
        ctx.close();
        System.out.println("=================================");
        
        // 对比：
        // 第一阶段：BeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans.xml"));
        // 第四阶段：AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(DemoConfig.class);
        // 未来阶段：LiteSpringApplication.run(DemoApplication.class, args);
    }
}

