package com.litespring.test.v1;

import com.litespring.core.BeanDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanDefinition测试类
 * 测试Bean定义的基本功能
 * 
 * @author lite-spring
 */
public class BeanDefinitionTest {
    
    /**
     * 测试：创建BeanDefinition
     */
    @Test
    public void testBeanDefinitionCreation() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
        
        assertEquals("com.litespring.test.v1.service.HelloService", bd.getBeanClassName());
        assertTrue(bd.isSingleton());  // 默认是单例
        assertFalse(bd.isPrototype());
        assertFalse(bd.isLazyInit());
    }
    
    /**
     * 测试：设置和获取Bean类名
     */
    @Test
    public void testBeanClassName() {
        BeanDefinition bd = new BeanDefinition("com.example.TestClass");
        
        assertEquals("com.example.TestClass", bd.getBeanClassName());
        
        bd.setBeanClassName("com.example.AnotherClass");
        assertEquals("com.example.AnotherClass", bd.getBeanClassName());
    }
    
    /**
     * 测试：单例作用域
     */
    @Test
    public void testSingletonScope() {
        BeanDefinition bd = new BeanDefinition("com.example.TestClass");
        
        // 默认是单例
        assertTrue(bd.isSingleton());
        assertFalse(bd.isPrototype());
        assertEquals("singleton", bd.getScope());
    }
    
    /**
     * 测试：原型作用域
     */
    @Test
    public void testPrototypeScope() {
        BeanDefinition bd = new BeanDefinition("com.example.TestClass");
        
        // 设置为原型
        bd.setScope("prototype");
        
        assertFalse(bd.isSingleton());
        assertTrue(bd.isPrototype());
        assertEquals("prototype", bd.getScope());
    }
    
    /**
     * 测试：作用域切换
     */
    @Test
    public void testScopeSwitch() {
        BeanDefinition bd = new BeanDefinition("com.example.TestClass");
        
        // 初始是单例
        assertTrue(bd.isSingleton());
        
        // 切换为原型
        bd.setScope("prototype");
        assertTrue(bd.isPrototype());
        
        // 切换回单例
        bd.setScope("singleton");
        assertTrue(bd.isSingleton());
    }
    
    /**
     * 测试：懒加载设置
     */
    @Test
    public void testLazyInit() {
        BeanDefinition bd = new BeanDefinition("com.example.TestClass");
        
        // 默认不是懒加载
        assertFalse(bd.isLazyInit());
        
        // 设置为懒加载
        bd.setLazyInit(true);
        assertTrue(bd.isLazyInit());
        
        // 取消懒加载
        bd.setLazyInit(false);
        assertFalse(bd.isLazyInit());
    }
    
    /**
     * 测试：初始化方法名
     */
    @Test
    public void testInitMethodName() {
        BeanDefinition bd = new BeanDefinition("com.example.TestClass");
        
        assertNull(bd.getInitMethodName());
        
        bd.setInitMethodName("init");
        assertEquals("init", bd.getInitMethodName());
    }
    
    /**
     * 测试：销毁方法名
     */
    @Test
    public void testDestroyMethodName() {
        BeanDefinition bd = new BeanDefinition("com.example.TestClass");
        
        assertNull(bd.getDestroyMethodName());
        
        bd.setDestroyMethodName("destroy");
        assertEquals("destroy", bd.getDestroyMethodName());
    }
}

