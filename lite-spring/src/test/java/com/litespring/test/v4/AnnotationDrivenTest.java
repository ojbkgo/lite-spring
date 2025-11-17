package com.litespring.test.v4;

import com.litespring.context.AnnotationConfigApplicationContext;
import com.litespring.test.v4.config.AppConfig;
import com.litespring.test.v4.service.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注解驱动综合测试
 * 测试完整的注解驱动流程
 * 
 * @author lite-spring
 */
public class AnnotationDrivenTest {
    
    /**
     * 测试：完整的注解驱动流程
     */
    @Test
    public void testCompleteAnnotationDriven() {
        System.out.println("\n========== 创建注解驱动容器 ==========");
        
        // 1. 创建容器
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        System.out.println("\n========== 容器创建完成 ==========");
        
        // 2. 按类型获取Bean
        UserService service = ctx.getBean(UserService.class);
        assertNotNull(service);
        
        // 3. 验证依赖注入
        assertNotNull(service.getUserDao());  // @Autowired注入
        assertEquals(3, service.getMaxRetry());  // @Value注入
        assertEquals("UserService", service.getServiceName());  // @Value注入
        
        // 4. 验证功能
        System.out.println("\n========== 调用业务方法 ==========");
        service.saveUser("Tom");
        
        System.out.println("\n========== 关闭容器 ==========");
        ctx.close();
        
        System.out.println("\n========== 测试完成 ==========");
    }
    
    /**
     * 测试：不再需要XML配置
     */
    @Test
    public void testNoXmlNeeded() {
        // 完全使用注解，不需要任何XML配置
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        UserService service = ctx.getBean(UserService.class);
        assertNotNull(service);
        
        // 所有依赖都自动装配
        assertNotNull(service.getUserDao());
        
        ctx.close();
    }
    
    /**
     * 测试：单例Bean共享
     */
    @Test
    public void testSingletonInAnnotationContext() {
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        UserService service1 = ctx.getBean(UserService.class);
        UserService service2 = ctx.getBean(UserService.class);
        
        // 默认是单例
        assertSame(service1, service2);
        assertSame(service1.getUserDao(), service2.getUserDao());
        
        ctx.close();
    }
}

