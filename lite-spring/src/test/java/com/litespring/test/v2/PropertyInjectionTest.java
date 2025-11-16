package com.litespring.test.v2;

import com.litespring.core.BeansException;
import com.litespring.core.DefaultBeanFactory_v2;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import com.litespring.test.v2.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 属性注入（Setter注入）测试
 * 
 * @author lite-spring
 */
public class PropertyInjectionTest {
    
    private DefaultBeanFactory_v2 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v2();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v2.xml"));
    }
    
    /**
     * 测试：Bean引用注入
     */
    @Test
    public void testBeanReferenceInjection() {
        UserService userService = factory.getBean("userService", UserService.class);
        
        assertNotNull(userService);
        assertNotNull(userService.getUserDao());
        
        // 验证依赖是否正确
        userService.saveUser("Tom");
    }
    
    /**
     * 测试：简单值注入（int）
     */
    @Test
    public void testIntValueInjection() {
        UserService userService = factory.getBean("userService", UserService.class);
        
        assertEquals(3, userService.getMaxRetry());
    }
    
    /**
     * 测试：简单值注入（String）
     */
    @Test
    public void testStringValueInjection() {
        UserService userService = factory.getBean("userService", UserService.class);
        
        assertEquals("UserService", userService.getServiceName());
    }
    
    /**
     * 测试：所有属性都被正确注入
     */
    @Test
    public void testAllPropertiesInjected() {
        UserService userService = factory.getBean("userService", UserService.class);
        
        // 验证所有属性都不为null或默认值
        assertNotNull(userService.getUserDao());
        assertEquals(3, userService.getMaxRetry());
        assertEquals("UserService", userService.getServiceName());
    }
    
    /**
     * 测试：单例Bean共享
     */
    @Test
    public void testSingletonShared() {
        UserService service1 = factory.getBean("userService", UserService.class);
        UserService service2 = factory.getBean("userService", UserService.class);
        
        assertSame(service1, service2);
        assertSame(service1.getUserDao(), service2.getUserDao());
    }
}

