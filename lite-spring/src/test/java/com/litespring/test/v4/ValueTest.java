package com.litespring.test.v4;

import com.litespring.context.AnnotationConfigApplicationContext;
import com.litespring.test.v4.config.AppConfig;
import com.litespring.test.v4.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Value注解测试
 * 
 * @author lite-spring
 */
public class ValueTest {
    
    private AnnotationConfigApplicationContext ctx;
    
    @BeforeEach
    public void setUp() {
        ctx = new AnnotationConfigApplicationContext(AppConfig.class);
    }
    
    @AfterEach
    public void tearDown() {
        if (ctx != null) {
            ctx.close();
        }
    }
    
    /**
     * 测试：@Value注入int值
     */
    @Test
    public void testValueIntInjection() {
        UserService service = ctx.getBean(UserService.class);
        
        // 验证@Value("3")被注入
        assertEquals(3, service.getMaxRetry());
    }
    
    /**
     * 测试：@Value注入String值
     */
    @Test
    public void testValueStringInjection() {
        UserService service = ctx.getBean(UserService.class);
        
        // 验证@Value("UserService")被注入
        assertEquals("UserService", service.getServiceName());
    }
}

