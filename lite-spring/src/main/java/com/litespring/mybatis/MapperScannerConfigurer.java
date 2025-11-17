package com.litespring.mybatis;

import com.litespring.core.BeanDefinition;
import com.litespring.core.BeanDefinitionRegistry;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper扫描配置器
 * 扫描指定包下的Mapper接口，自动注册为Bean
 * 
 * 这个类演示了如何扫描和注册MyBatis的Mapper接口
 * 
 * @author lite-spring
 */
public class MapperScannerConfigurer {
    
    private String basePackage;
    private SqlSessionFactory sqlSessionFactory;
    private BeanDefinitionRegistry registry;
    
    public MapperScannerConfigurer(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * 扫描Mapper接口
     */
    public void scan() throws Exception {
        if (basePackage == null) {
            throw new IllegalArgumentException("basePackage不能为null");
        }
        
        // 扫描包下的所有接口
        Set<Class<?>> mapperInterfaces = findMapperInterfaces(basePackage);
        
        // 注册每个Mapper
        for (Class<?> mapperInterface : mapperInterfaces) {
            registerMapper(mapperInterface);
        }
    }
    
    /**
     * 查找Mapper接口
     */
    private Set<Class<?>> findMapperInterfaces(String basePackage) throws Exception {
        Set<Class<?>> interfaces = new HashSet<>();
        
        String packagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(packagePath);
        
        if (resource == null) {
            return interfaces;
        }
        
        File directory = new File(resource.getFile());
        findMapperInterfaces(directory, basePackage, interfaces);
        
        return interfaces;
    }
    
    /**
     * 递归查找接口
     */
    private void findMapperInterfaces(File directory, String packageName, 
                                     Set<Class<?>> interfaces) throws Exception {
        
        if (!directory.exists()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                findMapperInterfaces(file, packageName + "." + file.getName(), interfaces);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + 
                    file.getName().substring(0, file.getName().length() - 6);
                
                Class<?> clazz = Class.forName(className);
                
                // 只要接口
                if (clazz.isInterface()) {
                    interfaces.add(clazz);
                }
            }
        }
    }
    
    /**
     * 注册Mapper
     */
    private void registerMapper(Class<?> mapperInterface) {
        // 创建MapperFactoryBean
        // MapperFactoryBean会从SqlSessionFactory获取Mapper实例
        
        String beanName = getBeanName(mapperInterface);
        
        // 注意：这里简化处理
        // 实际需要创建MapperFactoryBean的BeanDefinition
        // MapperFactoryBean.getObject()会返回Mapper的代理对象
        
        System.out.println("注册Mapper: " + mapperInterface.getName() + " as " + beanName);
    }
    
    /**
     * 获取Bean名称
     */
    private String getBeanName(Class<?> mapperInterface) {
        String className = mapperInterface.getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }
    
    // ==================== Getter和Setter ====================
    
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
    
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
}

