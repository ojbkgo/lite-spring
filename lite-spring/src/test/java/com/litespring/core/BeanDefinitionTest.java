package com.litespring.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanDefinition测试类
 * 
 * @author lite-spring
 */
public class BeanDefinitionTest {
    
    @Test
    public void testBeanDefinitionCreation() {
        BeanDefinition bd = new BeanDefinition("com.example.UserService");
        
        assertEquals("com.example.UserService", bd.getBeanClassName());
        assertTrue(bd.isSingleton());
        assertFalse(bd.isPrototype());
        assertFalse(bd.isLazyInit());
    }
    
    @Test
    public void testBeanScope() {
        BeanDefinition bd = new BeanDefinition("com.example.UserService");
        
        // 测试默认是单例
        assertTrue(bd.isSingleton());
        
        // 设置为原型
        bd.setScope("prototype");
        assertFalse(bd.isSingleton());
        assertTrue(bd.isPrototype());
    }
    
    @Test
    public void testLazyInit() {
        BeanDefinition bd = new BeanDefinition("com.example.UserService");
        
        assertFalse(bd.isLazyInit());
        
        bd.setLazyInit(true);
        assertTrue(bd.isLazyInit());
    }
    
    @Test
    public void testInitAndDestroyMethod() {
        BeanDefinition bd = new BeanDefinition("com.example.UserService");
        
        bd.setInitMethodName("init");
        bd.setDestroyMethodName("destroy");
        
        assertEquals("init", bd.getInitMethodName());
        assertEquals("destroy", bd.getDestroyMethodName());
    }
}

