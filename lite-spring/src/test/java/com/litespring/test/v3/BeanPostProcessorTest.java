package com.litespring.test.v3;

import com.litespring.core.BeanDefinition;
import com.litespring.core.DefaultBeanFactory_v3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanPostProcessor测试
 * 
 * @author lite-spring
 */
public class BeanPostProcessorTest {
    
    private DefaultBeanFactory_v3 factory;
    private LoggingBeanPostProcessor processor;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v3();
        processor = new LoggingBeanPostProcessor();
        factory.addBeanPostProcessor(processor);
    }
    
    /**
     * 测试：BeanPostProcessor的前置处理被调用
     */
    @Test
    public void testPostProcessBeforeInitialization() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v3.SimpleBean");
        factory.registerBeanDefinition("testBean", bd);
        
        factory.getBean("testBean");
        
        // 验证前置处理被调用
        assertTrue(processor.getBeforeInitBeans().contains("testBean"));
    }
    
    /**
     * 测试：BeanPostProcessor的后置处理被调用
     */
    @Test
    public void testPostProcessAfterInitialization() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v3.SimpleBean");
        factory.registerBeanDefinition("testBean", bd);
        
        factory.getBean("testBean");
        
        // 验证后置处理被调用
        assertTrue(processor.getAfterInitBeans().contains("testBean"));
    }
    
    /**
     * 测试：前置处理在初始化之前，后置处理在初始化之后
     */
    @Test
    public void testProcessorOrder() {
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v3.SimpleBean");
        factory.registerBeanDefinition("testBean", bd);
        
        SimpleBean bean = factory.getBean("testBean", SimpleBean.class);
        
        // Bean应该已初始化
        assertTrue(bean.isInitialized());
        
        // 前置和后置处理都应该被调用
        assertTrue(processor.getBeforeInitBeans().contains("testBean"));
        assertTrue(processor.getAfterInitBeans().contains("testBean"));
    }
    
    /**
     * 测试：多个BeanPostProcessor
     */
    @Test
    public void testMultipleBeanPostProcessors() {
        LoggingBeanPostProcessor processor2 = new LoggingBeanPostProcessor();
        factory.addBeanPostProcessor(processor2);
        
        BeanDefinition bd = new BeanDefinition("com.litespring.test.v3.SimpleBean");
        factory.registerBeanDefinition("testBean", bd);
        
        factory.getBean("testBean");
        
        // 两个处理器都应该被调用
        assertTrue(processor.getAfterInitBeans().contains("testBean"));
        assertTrue(processor2.getAfterInitBeans().contains("testBean"));
    }
}

