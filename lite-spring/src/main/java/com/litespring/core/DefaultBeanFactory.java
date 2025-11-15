package com.litespring.core;

import com.litespring.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的Bean工厂实现
 * 实现了Bean的注册、获取和创建功能
 * 
 * @author lite-spring
 */
public class DefaultBeanFactory implements BeanDefinitionRegistry, BeanFactory {
    
    /**
     * 存储Bean定义：beanName -> BeanDefinition
     */
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    
    /**
     * 存储单例Bean实例：beanName -> Bean实例
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition definition) {
        if (beanName == null || definition == null) {
            throw new IllegalArgumentException("beanName和definition不能为null");
        }
        
        // 如果已存在，抛出异常（也可以选择覆盖或忽略，这里选择抛异常更安全）
        if (beanDefinitions.containsKey(beanName)) {
            throw new BeansException("Bean定义已存在: " + beanName);
        }
        
        beanDefinitions.put(beanName, definition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        BeanDefinition bd = beanDefinitions.get(beanName);
        if (bd == null) {
            throw new BeansException("Bean定义不存在: " + beanName);
        }
        return bd;
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    @Override
    public Object getBean(String name) throws BeansException {
        if (name == null) {
            throw new IllegalArgumentException("Bean name不能为null");
        }
        
        BeanDefinition bd = beanDefinitions.get(name);
        if (bd == null) {
            throw new BeansException("Bean不存在: " + name);
        }

        // 单例Bean：从缓存获取或创建
        if (bd.isSingleton()) {
            Object bean = singletonObjects.get(name);
            if (bean == null) {
                // 双重检查锁定（后续优化可以考虑）
                synchronized (this.singletonObjects) {
                    bean = singletonObjects.get(name);
                    if (bean == null) {
                        bean = createBean(bd);
                        singletonObjects.put(name, bean);
                    }
                }
            }
            return bean;
        }
        
        // 原型Bean：每次创建新实例
        return createBean(bd);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        if (requiredType == null) {
            throw new IllegalArgumentException("requiredType不能为null");
        }
        
        Object bean = getBean(name);
        
        // 类型检查
        if (!requiredType.isInstance(bean)) {
            throw new BeansException("Bean类型不匹配，期望类型: " + requiredType.getName() 
                + ", 实际类型: " + bean.getClass().getName());
        }
        
        return requiredType.cast(bean);
    }

    @Override
    public boolean containsBean(String name) {
        // 检查是否包含Bean定义，而不是检查是否已创建实例
        return beanDefinitions.containsKey(name);
    }

    /**
     * 创建Bean实例
     * 通过反射调用无参构造函数创建对象
     * 
     * @param bd Bean定义
     * @return Bean实例
     * @throws BeansException 如果创建失败
     */
    private Object createBean(BeanDefinition bd) throws BeansException {
        String className = bd.getBeanClassName();
        if (className == null || className.trim().isEmpty()) {
            throw new BeansException("Bean的类名不能为空");
        }
        
        try {
            // 1. 加载类
            Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
            
            // 2. 通过无参构造函数创建实例
            return clazz.getConstructor().newInstance();
            
        } catch (ClassNotFoundException e) {
            throw new BeansException("类不存在: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new BeansException("类没有无参构造函数: " + className, e);
        } catch (InstantiationException e) {
            throw new BeansException("无法实例化类（可能是抽象类或接口）: " + className, e);
        } catch (IllegalAccessException e) {
            throw new BeansException("无法访问构造函数: " + className, e);
        } catch (InvocationTargetException e) {
            throw new BeansException("构造函数执行时抛出异常: " + className, e.getCause());
        }
    }
}
