package com.litespring.test.v3;

import com.litespring.core.DefaultBeanFactory_v3;
import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.XmlBeanDefinitionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 完整生命周期测试
 * 验证所有回调的调用顺序
 * 
 * @author lite-spring
 */
public class FullLifecycleTest {
    
    private DefaultBeanFactory_v3 factory;
    
    @BeforeEach
    public void setUp() {
        factory = new DefaultBeanFactory_v3();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(new ClassPathResource("beans-v3.xml"));
    }
    
    /**
     * 测试：完整的生命周期调用顺序
     */
    @Test
    public void testCompleteLifecycle() {
        // 添加BeanPostProcessor
        LoggingBeanPostProcessor processor = new LoggingBeanPostProcessor();
        factory.addBeanPostProcessor(processor);
        
        // 获取Bean
        LifecycleBean bean = factory.getBean("lifecycleBean", LifecycleBean.class);
        
        // 验证初始化阶段的调用顺序
        List<String> callbacks = bean.getCallbacks();
        
        assertEquals("1.构造函数", callbacks.get(0));
        assertEquals("2.属性注入", callbacks.get(1));
        assertEquals("3.BeanNameAware", callbacks.get(2));
        assertEquals("4.BeanFactoryAware", callbacks.get(3));
        // BeanPostProcessor前置处理在这里（不在Bean内部记录）
        assertEquals("5.InitializingBean", callbacks.get(4));
        assertEquals("6.customInit", callbacks.get(5));
        // BeanPostProcessor后置处理在这里
        
        // 验证BeanPostProcessor被调用
        assertTrue(processor.getBeforeInitBeans().contains("lifecycleBean"));
        assertTrue(processor.getAfterInitBeans().contains("lifecycleBean"));
        
        // 关闭容器
        factory.close();
        
        // 验证销毁阶段的调用顺序
        assertEquals("7.DisposableBean", callbacks.get(6));
        assertEquals("8.customDestroy", callbacks.get(7));
    }
    
    /**
     * 测试：所有属性都被正确设置
     */
    @Test
    public void testAllPropertiesSet() {
        LifecycleBean bean = factory.getBean("lifecycleBean", LifecycleBean.class);
        
        // 验证所有属性
        assertEquals("lifecycleBean", bean.getBeanName());
        assertNotNull(bean.getBeanFactory());
        assertEquals("test-value", bean.getProperty());
    }
    
    /**
     * 测试：打印完整的生命周期流程
     */
    @Test
    public void testPrintLifecycle() {
        LoggingBeanPostProcessor processor = new LoggingBeanPostProcessor();
        factory.addBeanPostProcessor(processor);
        
        System.out.println("\n========== 开始创建Bean ==========");
        LifecycleBean bean = factory.getBean("lifecycleBean", LifecycleBean.class);
        
        System.out.println("\n========== Bean创建完成 ==========");
        System.out.println("回调顺序: " + bean.getCallbacks());
        
        System.out.println("\n========== 关闭容器 ==========");
        factory.close();
        
        System.out.println("\n========== 容器已关闭 ==========");
        System.out.println("完整回调: " + bean.getCallbacks());
    }
}

