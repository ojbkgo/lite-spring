package com.litespring.test.v1;

import com.litespring.core.BeanFactory;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanFactory;
import com.litespring.test.v1.service.HelloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XmlBeanFactory测试类
 * 测试从XML配置文件创建Bean工厂
 * 
 * @author lite-spring
 */
public class XmlBeanFactoryTest {
    
    private BeanFactory factory;
    
    @BeforeEach
    public void setUp() {
        // 从XML配置文件创建Bean工厂
        factory = new XmlBeanFactory(new ClassPathResource("beans-v1.xml"));
    }
    
    /**
     * 测试：从XML加载Bean定义并创建Bean
     */
    @Test
    public void testGetBeanFromXml() {
        HelloService service = factory.getBean("helloService", HelloService.class);
        
        assertNotNull(service);
        assertEquals("Hello, Lite Spring!", service.sayHello());
    }
    
    /**
     * 测试：XML配置的单例Bean
     */
    @Test
    public void testSingletonBeanFromXml() {
        Object bean1 = factory.getBean("helloService");
        Object bean2 = factory.getBean("helloService");
        
        // 应该是同一个实例
        assertSame(bean1, bean2);
    }
    
    /**
     * 测试：XML配置的原型Bean
     */
    @Test
    public void testPrototypeBeanFromXml() {
        Object bean1 = factory.getBean("prototypeService");
        Object bean2 = factory.getBean("prototypeService");
        
        // 应该是不同的实例
        assertNotSame(bean1, bean2);
    }
    
    /**
     * 测试：检查Bean是否存在
     */
    @Test
    public void testContainsBeanFromXml() {
        assertTrue(factory.containsBean("helloService"));
        assertTrue(factory.containsBean("prototypeService"));
        assertFalse(factory.containsBean("nonExistent"));
    }
    
    /**
     * 测试：完整的使用场景
     */
    @Test
    public void testCompleteScenario() {
        // 1. 创建工厂（已在setUp中完成）
        
        // 2. 获取Bean
        HelloService service = factory.getBean("helloService", HelloService.class);
        
        // 3. 使用Bean
        String result = service.greet("World");
        
        // 4. 验证
        assertNotNull(service);
        assertEquals("Hello, World!", result);
        
        // 5. 验证单例
        HelloService service2 = factory.getBean("helloService", HelloService.class);
        assertSame(service, service2);
        
        // 6. 验证原型
        HelloService proto1 = factory.getBean("prototypeService", HelloService.class);
        HelloService proto2 = factory.getBean("prototypeService", HelloService.class);
        assertNotSame(proto1, proto2);
    }
}

