package com.litespring.demo;

/**
 * Demo应用启动类
 * 
 * 注意：这是目标代码，当前lite-spring还未实现相关功能
 * 这是第九阶段（自动配置）的目标效果
 * 
 * 目前需要手动创建容器并加载Bean
 * 
 * @author lite-spring
 */
// @LiteSpringBootApplication
public class DemoApplication {
    
    public static void main(String[] args) {
        // 最终目标：一行代码启动应用
        // LiteSpringApplication.run(DemoApplication.class, args);
        
        System.out.println("=================================");
        System.out.println("Lite Spring Demo Application");
        System.out.println("=================================");
        System.out.println("当前处于项目初始化阶段");
        System.out.println("请按照 docs/roadmap.md 中的规划逐步实现功能");
        System.out.println("=================================");
        
        // 第一阶段完成后，可以这样使用：
        // BeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans.xml"));
        // HelloService helloService = (HelloService) factory.getBean("helloService");
        // System.out.println(helloService.greet("World"));
    }
}

