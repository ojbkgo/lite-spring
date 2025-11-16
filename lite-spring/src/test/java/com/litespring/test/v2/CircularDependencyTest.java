package com.litespring.test.v2;

import com.litespring.core.*;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 循环依赖测试
 * 
 * @author lite-spring
 */
public class CircularDependencyTest {
    
    private DefaultBeanFactory_v2 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v2();
    }
    
    /**
     * 测试：Setter注入的循环依赖（应该能够解决）
     */
    @Test
    public void testSetterCircularDependency() {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v2.xml"));
        
        // 获取A
        CircularA a = factory.getBean("circularA", CircularA.class);
        assertNotNull(a);
        assertNotNull(a.getCircularB());
        
        // 获取B
        CircularB b = factory.getBean("circularB", CircularB.class);
        assertNotNull(b);
        assertNotNull(b.getCircularA());
        
        // 验证循环引用正确
        assertSame(a, b.getCircularA());
        assertSame(b, a.getCircularB());
    }
    
    /**
     * 测试：构造器注入的循环依赖（应该抛出异常）
     */
    @Test
    public void testConstructorCircularDependency() {
        // 创建两个Bean，构造器循环依赖
        BeanDefinition aBd = new BeanDefinition("com.litespring.test.v2.CircularA");
        aBd.getConstructorArgument().addArgumentValue(new RuntimeBeanReference("circularB"));
        factory.registerBeanDefinition("circularA", aBd);
        
        BeanDefinition bBd = new BeanDefinition("com.litespring.test.v2.CircularB");
        bBd.getConstructorArgument().addArgumentValue(new RuntimeBeanReference("circularA"));
        factory.registerBeanDefinition("circularB", bBd);
        
        // 应该抛出异常
        assertThrows(BeansException.class, () -> {
            factory.getBean("circularA");
        });
    }
    
    /**
     * 测试：原型Bean的循环依赖（应该抛出异常）
     */
    @Test
    public void testPrototypeCircularDependency() {
        // 创建两个原型Bean，setter循环依赖
        BeanDefinition aBd = new BeanDefinition("com.litespring.test.v2.CircularA");
        aBd.setScope("prototype");
        aBd.getPropertyValues().addPropertyValue("circularB", new RuntimeBeanReference("circularB"));
        factory.registerBeanDefinition("circularA", aBd);
        
        BeanDefinition bBd = new BeanDefinition("com.litespring.test.v2.CircularB");
        bBd.setScope("prototype");
        bBd.getPropertyValues().addPropertyValue("circularA", new RuntimeBeanReference("circularA"));
        factory.registerBeanDefinition("circularB", bBd);
        
        // 原型Bean的循环依赖也应该抛出异常
        assertThrows(BeansException.class, () -> {
            factory.getBean("circularA");
        });
    }
}

