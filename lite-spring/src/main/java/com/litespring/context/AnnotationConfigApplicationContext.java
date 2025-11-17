package com.litespring.context;

import com.litespring.annotation.ComponentScan;
import com.litespring.annotation.Configuration;
import com.litespring.core.*;

/**
 * 基于注解的应用上下文
 * 支持@Configuration和@ComponentScan
 * 
 * @author lite-spring
 */
public class AnnotationConfigApplicationContext {
    
    private final DefaultBeanFactory_v4 beanFactory;
    private final ClassPathBeanDefinitionScanner scanner;
    
    /**
     * 通过配置类创建容器
     */
    public AnnotationConfigApplicationContext(Class<?>... configClasses) {
        this.beanFactory = new DefaultBeanFactory_v4();
        this.scanner = new ClassPathBeanDefinitionScanner(beanFactory);
        
        // 注册配置类
        register(configClasses);
        
        // 刷新容器
        refresh();
    }
    
    /**
     * 通过包扫描创建容器
     */
    public AnnotationConfigApplicationContext(String... basePackages) {
        this.beanFactory = new DefaultBeanFactory_v4();
        this.scanner = new ClassPathBeanDefinitionScanner(beanFactory);
        
        // 扫描包
        scan(basePackages);
        
        // 刷新容器
        refresh();
    }
    
    /**
     * 注册配置类
     */
    private void register(Class<?>... configClasses) {
        for (Class<?> configClass : configClasses) {
            // 检查是否有@Configuration注解
            if (!configClass.isAnnotationPresent(Configuration.class)) {
                throw new BeansException("类必须标注@Configuration: " + configClass.getName());
            }
            
            // 注册配置类本身
            String beanName = getBeanName(configClass);
            BeanDefinition bd = new BeanDefinition(configClass.getName());
            beanFactory.registerBeanDefinition(beanName, bd);
        }
    }
    
    /**
     * 扫描包
     */
    private void scan(String... basePackages) {
        scanner.scan(basePackages);
    }
    
    /**
     * 刷新容器
     */
    private void refresh() {
        // 1. 注册内置的BeanPostProcessor
        registerBeanPostProcessors();
        
        // 2. 处理@ComponentScan注解
        processComponentScan();
        
        // 3. 处理@Configuration类的@Bean方法
        processConfigurationClasses();
        
        // 4. 实例化所有非懒加载的单例Bean
        finishBeanFactoryInitialization();
    }
    
    /**
     * 注册BeanPostProcessor
     */
    private void registerBeanPostProcessors() {
        // 注册AutowiredAnnotationBeanPostProcessor
        AutowiredAnnotationBeanPostProcessor autowiredProcessor = 
            new AutowiredAnnotationBeanPostProcessor();
        autowiredProcessor.setBeanFactory(beanFactory);
        beanFactory.addBeanPostProcessor(autowiredProcessor);
        
        // 注册ValueAnnotationBeanPostProcessor
        ValueAnnotationBeanPostProcessor valueProcessor = 
            new ValueAnnotationBeanPostProcessor();
        beanFactory.addBeanPostProcessor(valueProcessor);
    }
    
    /**
     * 处理@ComponentScan注解
     */
    private void processComponentScan() {
        // 获取所有已注册的Bean（主要是配置类）
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                
                // 检查是否有@ComponentScan注解
                ComponentScan componentScan = clazz.getAnnotation(ComponentScan.class);
                if (componentScan != null) {
                    // 获取要扫描的包
                    String[] packages = componentScan.value();
                    if (packages.length == 0) {
                        packages = componentScan.basePackages();
                    }
                    
                    if (packages.length > 0) {
                        // 执行扫描
                        scanner.scan(packages);
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new BeansException("加载类失败: " + bd.getBeanClassName(), e);
            }
        }
    }
    
    /**
     * 处理@Configuration类
     */
    private void processConfigurationClasses() {
        // 获取所有Bean定义
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                
                // 检查是否有@Configuration注解
                if (clazz.isAnnotationPresent(Configuration.class)) {
                    processConfigurationClass(beanName, clazz);
                }
            } catch (ClassNotFoundException e) {
                throw new BeansException("加载配置类失败: " + bd.getBeanClassName(), e);
            }
        }
    }
    
    /**
     * 处理单个配置类
     */
    private void processConfigurationClass(String configBeanName, Class<?> configClass) {
        // 第四阶段简化：暂不实现@Bean方法处理
        // 后续可扩展
        // TODO: 处理@Bean注解的方法
    }
    
    /**
     * 完成BeanFactory初始化（实例化所有单例Bean）
     */
    private void finishBeanFactoryInitialization() {
        // 获取所有Bean名称
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        
        // 实例化所有非懒加载的单例Bean
        for (String beanName : beanNames) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            
            // 只实例化单例且非懒加载的Bean
            if (bd.isSingleton() && !bd.isLazyInit()) {
                beanFactory.getBean(beanName);
            }
        }
    }
    
    /**
     * 获取Bean名称
     */
    private String getBeanName(Class<?> clazz) {
        Configuration config = clazz.getAnnotation(Configuration.class);
        if (config != null && !config.value().isEmpty()) {
            return config.value();
        }
        
        String className = clazz.getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }
    
    // ==================== ApplicationContext接口方法 ====================
    
    public Object getBean(String name) {
        return beanFactory.getBean(name);
    }
    
    public <T> T getBean(String name, Class<T> requiredType) {
        return beanFactory.getBean(name, requiredType);
    }
    
    public <T> T getBean(Class<T> requiredType) {
        return beanFactory.getBean(requiredType);
    }
    
    public boolean containsBean(String name) {
        return beanFactory.containsBean(name);
    }
    
    public void close() {
        beanFactory.close();
    }
}

