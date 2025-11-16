package com.litespring.core;

import com.litespring.util.ClassUtils;
import com.litespring.util.SimpleTypeConverter;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的Bean工厂实现 - 第二阶段增强版
 * 新增功能：
 * 1. 属性注入（Setter注入）
 * 2. 构造器注入
 * 3. 三级缓存处理循环依赖
 * 
 * @author lite-spring
 */
public class DefaultBeanFactory_v2 implements BeanDefinitionRegistry, BeanFactory {
    
    /**
     * 一级缓存：存储完全初始化好的单例Bean
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    /**
     * 二级缓存：存储早期暴露的Bean（已实例化，未完成属性注入）
     */
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    
    /**
     * 三级缓存：存储Bean工厂（用于延迟创建代理对象）
     */
    private final Map<String, ObjectFactory> singletonFactories = new HashMap<>();
    
    /**
     * 存储Bean定义
     */
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    
    /**
     * 正在创建的Bean集合（用于检测循环依赖）
     */
    private final Set<String> singletonsCurrentlyInCreation = 
        Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    /**
     * 类型转换器
     */
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    
    // ==================== BeanDefinitionRegistry接口实现 ====================
    
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition definition) {
        if (beanName == null || definition == null) {
            throw new IllegalArgumentException("beanName和definition不能为null");
        }
        
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
    
    // ==================== BeanFactory接口实现 ====================
    
    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name);
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        Object bean = getBean(name);
        
        if (!requiredType.isInstance(bean)) {
            throw new BeansException("Bean类型不匹配，期望类型: " + requiredType.getName() 
                + ", 实际类型: " + bean.getClass().getName());
        }
        
        return requiredType.cast(bean);
    }
    
    @Override
    public boolean containsBean(String name) {
        return beanDefinitions.containsKey(name);
    }
    
    // ==================== 核心方法 ====================
    
    /**
     * 获取Bean的核心方法（支持三级缓存）
     */
    private Object doGetBean(String beanName) {
        if (beanName == null) {
            throw new IllegalArgumentException("Bean name不能为null");
        }
        
        // 1. 尝试从缓存获取（三级缓存）
        Object sharedInstance = getSingleton(beanName);
        if (sharedInstance != null) {
            return sharedInstance;
        }
        
        // 2. 获取Bean定义
        BeanDefinition bd = beanDefinitions.get(beanName);
        if (bd == null) {
            throw new BeansException("Bean不存在: " + beanName);
        }
        
        // 3. 单例Bean
        if (bd.isSingleton()) {
            // 检测构造器循环依赖
            if (bd.hasConstructorArgumentValues() && isSingletonCurrentlyInCreation(beanName)) {
                throw new BeansException(
                    "检测到构造器循环依赖，无法解决: " + beanName
                );
            }
            
            // 创建单例Bean
            sharedInstance = getSingleton(beanName, () -> {
                return createBean(beanName, bd);
            });
            return sharedInstance;
        }
        
        // 4. 原型Bean（每次都创建新的）
        if (bd.isPrototype()) {
            // 检测原型Bean的循环依赖
            if (isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeansException(
                    "检测到原型Bean的循环依赖，无法解决: " + beanName
                );
            }
            
            try {
                beforePrototypeCreation(beanName);
                return createBean(beanName, bd);
            } finally {
                afterPrototypeCreation(beanName);
            }
        }
        
        throw new BeansException("不支持的Bean scope: " + bd.getScope());
    }
    
    /**
     * 获取单例Bean（支持三级缓存）
     */
    private Object getSingleton(String beanName) {
        // 从一级缓存获取
        Object singletonObject = singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                // 从二级缓存获取
                singletonObject = earlySingletonObjects.get(beanName);
                if (singletonObject == null) {
                    // 从三级缓存获取
                    ObjectFactory singletonFactory = singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        singletonObject = singletonFactory.getObject();
                        // 升级到二级缓存
                        earlySingletonObjects.put(beanName, singletonObject);
                        singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return singletonObject;
    }
    
    /**
     * 获取单例Bean（带工厂）
     */
    private Object getSingleton(String beanName, ObjectFactory objectFactory) {
        synchronized (this.singletonObjects) {
            Object singletonObject = singletonObjects.get(beanName);
            if (singletonObject == null) {
                // 标记为正在创建
                beforeSingletonCreation(beanName);
                
                try {
                    // 调用工厂创建Bean
                    singletonObject = objectFactory.getObject();
                    
                    // 从二级缓存移除，放入一级缓存
                    earlySingletonObjects.remove(beanName);
                    singletonFactories.remove(beanName);
                    singletonObjects.put(beanName, singletonObject);
                    
                } catch (Exception e) {
                    throw new BeansException("创建Bean失败: " + beanName, e);
                } finally {
                    // 移除正在创建标记
                    afterSingletonCreation(beanName);
                }
            }
            return singletonObject;
        }
    }
    
    /**
     * 创建Bean实例
     */
    private Object createBean(String beanName, BeanDefinition bd) {
        // 1. 实例化Bean
        Object bean = instantiateBean(bd);
        
        // 2. 单例Bean提前暴露（放入三级缓存，支持循环依赖）
        if (bd.isSingleton() && !bd.hasConstructorArgumentValues()) {
            addSingletonFactory(beanName, () -> bean);
        }
        
        // 3. 属性注入
        populateBean(beanName, bean, bd);
        
        return bean;
    }
    
    /**
     * 实例化Bean
     */
    private Object instantiateBean(BeanDefinition bd) {
        String className = bd.getBeanClassName();
        if (className == null || className.trim().isEmpty()) {
            throw new BeansException("Bean的类名不能为空");
        }
        
        try {
            Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
            
            // 判断是否有构造器参数
            if (bd.hasConstructorArgumentValues()) {
                return instantiateUsingConstructor(clazz, bd.getConstructorArgument());
            } else {
                return clazz.getConstructor().newInstance();
            }
            
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
    
    /**
     * 使用构造器实例化Bean
     */
    private Object instantiateUsingConstructor(Class<?> clazz, ConstructorArgument constructorArg) {
        List<ConstructorArgument.ValueHolder> valueHolders = constructorArg.getArgumentValues();
        List<Object> resolvedArgs = new ArrayList<>();
        
        // 解析构造器参数
        for (ConstructorArgument.ValueHolder valueHolder : valueHolders) {
            Object resolvedValue = resolveValueIfNecessary(valueHolder.getValue());
            resolvedArgs.add(resolvedValue);
        }
        
        // 获取参数类型数组
        Class<?>[] paramTypes = new Class<?>[resolvedArgs.size()];
        for (int i = 0; i < resolvedArgs.size(); i++) {
            paramTypes[i] = resolvedArgs.get(i).getClass();
        }
        
        try {
            // 查找匹配的构造器
            Constructor<?> constructor = findMatchingConstructor(clazz, paramTypes);
            return constructor.newInstance(resolvedArgs.toArray());
            
        } catch (Exception e) {
            throw new BeansException("使用构造器创建Bean失败: " + clazz.getName(), e);
        }
    }
    
    /**
     * 查找匹配的构造器（支持类型兼容）
     */
    private Constructor<?> findMatchingConstructor(Class<?> clazz, Class<?>[] paramTypes) 
            throws NoSuchMethodException {
        // 先尝试精确匹配
        try {
            return clazz.getConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            // 精确匹配失败，尝试兼容匹配
        }
        
        // 兼容匹配：查找参数个数相同且类型兼容的构造器
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] ctorParamTypes = constructor.getParameterTypes();
            if (ctorParamTypes.length == paramTypes.length) {
                boolean match = true;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (!isAssignable(ctorParamTypes[i], paramTypes[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return constructor;
                }
            }
        }
        
        throw new NoSuchMethodException(
            "找不到匹配的构造器: " + clazz.getName() + 
            ", 参数类型: " + Arrays.toString(paramTypes)
        );
    }
    
    /**
     * 判断类型是否兼容
     */
    private boolean isAssignable(Class<?> target, Class<?> source) {
        // 处理基本类型和包装类型
        if (target.isPrimitive()) {
            if (target == int.class && source == Integer.class) return true;
            if (target == long.class && source == Long.class) return true;
            if (target == double.class && source == Double.class) return true;
            if (target == float.class && source == Float.class) return true;
            if (target == boolean.class && source == Boolean.class) return true;
            if (target == short.class && source == Short.class) return true;
            if (target == byte.class && source == Byte.class) return true;
            if (target == char.class && source == Character.class) return true;
        }
        return target.isAssignableFrom(source);
    }
    
    /**
     * 填充Bean属性（Setter注入）
     */
    private void populateBean(String beanName, Object bean, BeanDefinition bd) {
        PropertyValues pvs = bd.getPropertyValues();
        if (pvs.isEmpty()) {
            return;
        }
        
        try {
            // 获取Bean信息
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
            
            // 遍历属性值
            for (PropertyValue pv : pvs.getPropertyValues()) {
                String propertyName = pv.getName();
                Object value = pv.getValue();
                
                // 解析属性值
                Object resolvedValue = resolveValueIfNecessary(value);
                
                // 查找对应的PropertyDescriptor
                PropertyDescriptor pd = findPropertyDescriptor(pds, propertyName);
                if (pd == null || pd.getWriteMethod() == null) {
                    throw new BeansException(
                        "Bean[" + beanName + "]没有属性[" + propertyName + "]的setter方法"
                    );
                }
                
                // 类型转换
                Class<?> propertyType = pd.getPropertyType();
                Object convertedValue = convertValueIfNecessary(resolvedValue, propertyType);
                
                // 调用setter方法
                Method writeMethod = pd.getWriteMethod();
                writeMethod.invoke(bean, convertedValue);
            }
            
        } catch (Exception e) {
            throw new BeansException("属性注入失败: " + beanName, e);
        }
    }
    
    /**
     * 查找PropertyDescriptor
     */
    private PropertyDescriptor findPropertyDescriptor(PropertyDescriptor[] pds, String propertyName) {
        for (PropertyDescriptor pd : pds) {
            if (pd.getName().equals(propertyName)) {
                return pd;
            }
        }
        return null;
    }
    
    /**
     * 解析属性值
     */
    private Object resolveValueIfNecessary(Object value) {
        if (value instanceof RuntimeBeanReference) {
            // Bean引用，递归获取
            RuntimeBeanReference ref = (RuntimeBeanReference) value;
            return getBean(ref.getBeanName());
            
        } else if (value instanceof TypedStringValue) {
            // 字符串值，直接返回（类型转换在后面处理）
            return ((TypedStringValue) value).getValue();
        }
        
        return value;
    }
    
    /**
     * 类型转换
     */
    private Object convertValueIfNecessary(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // 如果已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // 如果是字符串，使用类型转换器
        if (value instanceof String) {
            return typeConverter.convertIfNecessary((String) value, targetType);
        }
        
        return value;
    }
    
    /**
     * 添加到三级缓存
     */
    private void addSingletonFactory(String beanName, ObjectFactory objectFactory) {
        synchronized (this.singletonObjects) {
            if (!singletonObjects.containsKey(beanName)) {
                singletonFactories.put(beanName, objectFactory);
                earlySingletonObjects.remove(beanName);
            }
        }
    }
    
    // ==================== 循环依赖检测相关 ====================
    
    private void beforeSingletonCreation(String beanName) {
        if (!singletonsCurrentlyInCreation.add(beanName)) {
            throw new BeansException("Bean正在创建中: " + beanName);
        }
    }
    
    private void afterSingletonCreation(String beanName) {
        if (!singletonsCurrentlyInCreation.remove(beanName)) {
            throw new IllegalStateException("Singleton " + beanName + " 不在创建中");
        }
    }
    
    private boolean isSingletonCurrentlyInCreation(String beanName) {
        return singletonsCurrentlyInCreation.contains(beanName);
    }
    
    // 原型Bean循环依赖检测（使用ThreadLocal）
    private final ThreadLocal<Set<String>> prototypesCurrentlyInCreation = 
        ThreadLocal.withInitial(HashSet::new);
    
    private void beforePrototypeCreation(String beanName) {
        Set<String> creating = prototypesCurrentlyInCreation.get();
        if (!creating.add(beanName)) {
            throw new BeansException(
                "检测到原型Bean的循环依赖: " + beanName + ", 当前创建中: " + creating
            );
        }
    }
    
    private void afterPrototypeCreation(String beanName) {
        prototypesCurrentlyInCreation.get().remove(beanName);
    }
    
    private boolean isPrototypeCurrentlyInCreation(String beanName) {
        return prototypesCurrentlyInCreation.get().contains(beanName);
    }
    
    /**
     * 对象工厂接口（用于三级缓存）
     */
    @FunctionalInterface
    public interface ObjectFactory {
        Object getObject() throws BeansException;
    }
}

