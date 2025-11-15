package com.litespring.core.io;

import com.litespring.core.DefaultBeanFactory;

/**
 * 基于XML配置的Bean工厂
 * 组合了DefaultBeanFactory和XmlBeanDefinitionReader
 * 提供便捷的使用方式
 * 
 * @author lite-spring
 */
public class XmlBeanFactory extends DefaultBeanFactory {
    
    /**
     * 从Resource加载Bean定义
     */
    public XmlBeanFactory(Resource resource) {
        // 创建XML读取器
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);
        
        // 加载Bean定义
        reader.loadBeanDefinitions(resource);
    }
}

