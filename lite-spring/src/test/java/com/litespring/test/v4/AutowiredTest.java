package com.litespring.test.v4;

import com.litespring.context.AnnotationConfigApplicationContext;
import com.litespring.test.v4.config.AppConfig;
import com.litespring.test.v4.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Autowired注解测试
 * 
 * @author lite-spring
 */
public class AutowiredTest {
    
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
     * 测试：@Autowired自动装配Bean引用
     */
    @Test
    public void testAutowiredBeanReference() {
        UserService service = ctx.getBean(UserService.class);
        
        // 验证UserDao被自动注入
        assertNotNull(service.getUserDao());
    }
    
    /**
     * 测试：@Autowired按类型匹配
     */
    @Test
    public void testAutowiredByType() {
        UserService service = ctx.getBean(UserService.class);
        
        // UserDao接口只有一个实现UserDaoImpl
        // 应该能自动匹配
        assertNotNull(service.getUserDao());
        
        // 验证功能正常
        service.saveUser("Tom");
    }
}

