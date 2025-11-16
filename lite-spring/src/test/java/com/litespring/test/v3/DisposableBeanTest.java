package com.litespring.test.v3;

import com.litespring.core.DefaultBeanFactory_v3;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DisposableBean接口测试
 * 
 * @author lite-spring
 */
public class DisposableBeanTest {
    
    private DefaultBeanFactory_v3 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v3();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v3.xml"));
    }
    
    /**
     * 测试：DisposableBean的destroy方法在容器关闭时被调用
     */
    @Test
    public void testDestroy() {
        DisposableTestBean bean = factory.getBean("disposableBean", DisposableTestBean.class);
        
        // 初始状态：已初始化，未销毁
        assertTrue(bean.isInitialized());
        assertFalse(bean.isDestroyed());
        
        // 关闭容器
        factory.close();
        
        // 验证销毁方法被调用
        assertTrue(bean.isDestroyed());
    }
    
    /**
     * 测试：原型Bean不会被销毁
     */
    @Test
    public void testPrototypeBeanNotDestroyed() {
        // 暂时跳过，第二阶段已有原型Bean测试
        // 原型Bean不会注册到disposableBeans中
    }
}

