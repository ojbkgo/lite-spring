package com.litespring.test.v3;

import com.litespring.core.DefaultBeanFactory_v3;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aware接口测试
 * 
 * @author lite-spring
 */
public class AwareInterfaceTest {
    
    private DefaultBeanFactory_v3 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v3();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v3.xml"));
    }
    
    /**
     * 测试：BeanNameAware接口
     */
    @Test
    public void testBeanNameAware() {
        LifecycleBean bean = factory.getBean("lifecycleBean", LifecycleBean.class);
        
        // 验证setBeanName被调用
        assertEquals("lifecycleBean", bean.getBeanName());
    }
    
    /**
     * 测试：BeanFactoryAware接口
     */
    @Test
    public void testBeanFactoryAware() {
        LifecycleBean bean = factory.getBean("lifecycleBean", LifecycleBean.class);
        
        // 验证setBeanFactory被调用
        assertNotNull(bean.getBeanFactory());
        assertSame(factory, bean.getBeanFactory());
    }
    
    /**
     * 测试：Aware接口在属性注入之后调用
     */
    @Test
    public void testAwareAfterPropertySet() {
        LifecycleBean bean = factory.getBean("lifecycleBean", LifecycleBean.class);
        
        // 验证调用顺序
        List<String> callbacks = bean.getCallbacks();
        
        // 属性注入应该在Aware接口之前
        int propertyIndex = callbacks.indexOf("2.属性注入");
        int awareIndex = callbacks.indexOf("3.BeanNameAware");
        
        assertTrue(propertyIndex < awareIndex);
    }
}

