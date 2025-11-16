package com.litespring.test.v3;

import com.litespring.core.DefaultBeanFactory_v3;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自定义init-method和destroy-method测试
 * 
 * @author lite-spring
 */
public class CustomInitDestroyTest {
    
    private DefaultBeanFactory_v3 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v3();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v3.xml"));
    }
    
    /**
     * 测试：自定义init-method被调用
     */
    @Test
    public void testCustomInitMethod() {
        CustomInitDestroyBean bean = factory.getBean("customBean", CustomInitDestroyBean.class);
        
        // 验证初始化方法被调用
        assertTrue(bean.isInitialized());
    }
    
    /**
     * 测试：自定义destroy-method被调用
     */
    @Test
    public void testCustomDestroyMethod() {
        CustomInitDestroyBean bean = factory.getBean("customBean", CustomInitDestroyBean.class);
        
        assertFalse(bean.isDestroyed());
        
        // 关闭容器
        factory.close();
        
        // 验证销毁方法被调用
        assertTrue(bean.isDestroyed());
    }
}

