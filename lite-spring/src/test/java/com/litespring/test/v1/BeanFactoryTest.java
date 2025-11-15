package com.litespring.test.v1;

import com.litespring.core.BeanDefinition;
import com.litespring.core.BeanFactory;
import com.litespring.core.BeansException;
import com.litespring.core.DefaultBeanFactory;
import com.litespring.test.v1.service.HelloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean工厂测试类
 * 测试第一阶段的核心功能：Bean的注册、获取和创建
 * 
 * @author lite-spring
 */
public class BeanFactoryTest {
    
    private DefaultBeanFactory factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory();
    }
    
    /**
     * 测试：注册和获取Bean定义
     */
    @Test
    public void testRegisterBeanDefinition() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        factory.registerBeanDefinition("helloService", bd);
        
        // 验证能获取到Bean定义
        BeanDefinition bd2 = factory.getBeanDefinition("helloService");
        assertNotNull(bd2);
        assertEquals("com.litespring.test.v1.service.HelloService", bd2.getBeanClassName());
    }
    
    /**
     * 测试：重复注册Bean定义应该抛出异常
     */
    @Test
    public void testRegisterDuplicateBeanDefinition() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        factory.registerBeanDefinition("helloService", bd);
        
        // 重复注册应该抛出异常
        assertThrows(BeansException.class, () -> {
            factory.registerBeanDefinition("helloService", bd);
        });
    }
    
    /**
     * 测试：获取不存在的Bean定义应该抛出异常
     */
    @Test
    public void testGetNonExistentBeanDefinition() {
        assertThrows(BeansException.class, () -> {
            factory.getBeanDefinition("nonExistent");
        });
    }
    
    /**
     * 测试：检查Bean定义是否存在
     */
    @Test
    public void testContainsBeanDefinition() {
        assertFalse(factory.containsBeanDefinition("helloService"));
        
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        factory.registerBeanDefinition("helloService", bd);
        
        assertTrue(factory.containsBeanDefinition("helloService"));
    }
    
    /**
     * 测试：创建和获取Bean实例
     */
    @Test
    public void testGetBean() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        factory.registerBeanDefinition("helloService", bd);
        
        Object bean = factory.getBean("helloService");
        
        assertNotNull(bean);
        assertTrue(bean instanceof HelloService);
        
        HelloService service = (HelloService) bean;
        assertEquals("Hello, Lite Spring!", service.sayHello());
    }
    
    /**
     * 测试：获取不存在的Bean应该抛出异常
     */
    @Test
    public void testGetNonExistentBean() {
        assertThrows(BeansException.class, () -> {
            factory.getBean("nonExistent");
        });
    }
    
    /**
     * 测试：单例Bean应该返回同一个实例
     */
    @Test
    public void testSingletonScope() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        bd.setScope("singleton");  // 显式设置为单例（虽然默认就是）
        factory.registerBeanDefinition("helloService", bd);
        
        Object bean1 = factory.getBean("helloService");
        Object bean2 = factory.getBean("helloService");
        
        // 单例Bean应该是同一个实例
        assertSame(bean1, bean2);
    }
    
    /**
     * 测试：原型Bean每次应该返回新实例
     */
    @Test
    public void testPrototypeScope() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        bd.setScope("prototype");
        factory.registerBeanDefinition("helloService", bd);
        
        Object bean1 = factory.getBean("helloService");
        Object bean2 = factory.getBean("helloService");
        
        // 原型Bean应该是不同的实例
        assertNotSame(bean1, bean2);
    }
    
    /**
     * 测试：按类型获取Bean
     */
    @Test
    public void testGetBeanWithType() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        factory.registerBeanDefinition("helloService", bd);
        
        // 使用泛型方法获取Bean，避免强制类型转换
        HelloService service = factory.getBean("helloService", HelloService.class);
        
        assertNotNull(service);
        assertEquals("Hello, Lite Spring!", service.sayHello());
    }
    
    /**
     * 测试：类型不匹配时应该抛出异常
     */
    @Test
    public void testGetBeanWithWrongType() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        factory.registerBeanDefinition("helloService", bd);
        
        // 期望HelloService类型，但要求String类型，应该抛出异常
        assertThrows(BeansException.class, () -> {
            factory.getBean("helloService", String.class);
        });
    }
    
    /**
     * 测试：检查Bean是否存在
     */
    @Test
    public void testContainsBean() {
        assertFalse(factory.containsBean("helloService"));
        
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        factory.registerBeanDefinition("helloService", bd);
        
        // 注册后就应该返回true（即使还没创建实例）
        assertTrue(factory.containsBean("helloService"));
    }
    
    /**
     * 测试：类不存在时应该抛出异常
     */
    @Test
    public void testGetBeanWithNonExistentClass() {
        BeanDefinition bd = new BeanDefinition("com.nonexistent.NonExistentClass");
        factory.registerBeanDefinition("test", bd);
        
        assertThrows(BeansException.class, () -> {
            factory.getBean("test");
        });
    }
    
    /**
     * 测试：没有无参构造函数时应该抛出异常
     */
    @Test
    public void testGetBeanWithNoDefaultConstructor() {
        // Integer没有无参构造函数
        BeanDefinition bd = new BeanDefinition("java.lang.Integer");
        factory.registerBeanDefinition("integer", bd);
        
        assertThrows(BeansException.class, () -> {
            factory.getBean("integer");
        });
    }
}

