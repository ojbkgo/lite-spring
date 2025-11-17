package com.litespring.test.v4;

import com.litespring.context.AnnotationConfigApplicationContext;
import com.litespring.test.v4.config.AppConfig;
import com.litespring.test.v4.dao.UserDao;
import com.litespring.test.v4.dao.UserDaoImpl;
import com.litespring.test.v4.service.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ComponentScan注解测试
 * 
 * @author lite-spring
 */
public class ComponentScanTest {
    
    /**
     * 测试：通过@ComponentScan自动扫描和注册Bean
     */
    @Test
    public void testComponentScan() {
        // 创建容器，传入配置类
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        // 验证扫描到的Bean
        assertTrue(ctx.containsBean("userDaoImpl"));  // @Repository
        assertTrue(ctx.containsBean("userService"));  // @Service
        
        ctx.close();
    }
    
    /**
     * 测试：获取扫描到的Bean
     */
    @Test
    public void testGetScannedBean() {
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        // 按名称获取
        UserService service = ctx.getBean("userService", UserService.class);
        assertNotNull(service);
        
        UserDaoImpl dao = ctx.getBean("userDaoImpl", UserDaoImpl.class);
        assertNotNull(dao);
        
        ctx.close();
    }
    
    /**
     * 测试：按类型获取Bean
     */
    @Test
    public void testGetBeanByType() {
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        // 按类型获取
        UserService service = ctx.getBean(UserService.class);
        assertNotNull(service);
        
        UserDao dao = ctx.getBean(UserDao.class);
        assertNotNull(dao);
        assertTrue(dao instanceof UserDaoImpl);
        
        ctx.close();
    }
}

