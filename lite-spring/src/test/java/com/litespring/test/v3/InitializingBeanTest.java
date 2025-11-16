package com.litespring.test.v3;

import com.litespring.core.DefaultBeanFactory_v3;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InitializingBean接口测试
 * 
 * @author lite-spring
 */
public class InitializingBeanTest {
    
    private DefaultBeanFactory_v3 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v3();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v3.xml"));
    }
    
    /**
     * 测试：InitializingBean接口的afterPropertiesSet方法被调用
     */
    @Test
    public void testAfterPropertiesSet() {
        SimpleBean bean = factory.getBean("simpleBean", SimpleBean.class);
        
        // 验证初始化方法被调用
        assertTrue(bean.isInitialized());
    }
    
    /**
     * 测试：初始化在属性注入之后
     */
    @Test
    public void testInitAfterPropertySet() {
        SimpleBean bean = factory.getBean("simpleBean", SimpleBean.class);
        
        // 属性应该已注入
        assertEquals("TestBean", bean.getName());
        // 初始化方法应该已调用
        assertTrue(bean.isInitialized());
    }
}

