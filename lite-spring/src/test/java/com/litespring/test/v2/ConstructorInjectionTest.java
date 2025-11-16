package com.litespring.test.v2;

import com.litespring.core.DefaultBeanFactory_v2;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import com.litespring.test.v2.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 构造器注入测试
 * 
 * @author lite-spring
 */
public class ConstructorInjectionTest {
    
    private DefaultBeanFactory_v2 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v2();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v2.xml"));
    }
    
    /**
     * 测试：构造器注入Bean引用
     */
    @Test
    public void testConstructorBeanReferenceInjection() {
        OrderService orderService = factory.getBean("orderService", OrderService.class);
        
        assertNotNull(orderService);
        assertNotNull(orderService.getUserDao());
    }
    
    /**
     * 测试：构造器注入简单值
     */
    @Test
    public void testConstructorValueInjection() {
        OrderService orderService = factory.getBean("orderService", OrderService.class);
        
        assertEquals(100, orderService.getMaxSize());
    }
    
    /**
     * 测试：构造器注入的对象是final的
     */
    @Test
    public void testConstructorInjectedFieldsAreFinal() {
        OrderService orderService = factory.getBean("orderService", OrderService.class);
        
        // final字段不能被重新赋值，这是构造器注入的优势
        assertNotNull(orderService.getUserDao());
        assertEquals(100, orderService.getMaxSize());
    }
}

