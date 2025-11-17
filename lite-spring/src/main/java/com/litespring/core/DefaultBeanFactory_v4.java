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
 * 默认的Bean工厂实现 - 第四阶段增强版
 * 在第三阶段基础上新增：
 * 1. 按类型获取Bean
 * 2. 获取所有Bean名称
 * 3. 按类型获取所有Bean
 * 
 * @author lite-spring
 */
public class DefaultBeanFactory_v4 implements BeanDefinitionRegistry, BeanFactory {
    
    // ==================== 三级缓存 ====================
    
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    private final Map<String, ObjectFactory> singletonFactories = new HashMap<>();
    
    // ==================== 其他存储 ====================
    
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = 
        Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    private final Map<String, Object> disposableBeans = new LinkedHashMap<>();
    
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    
    // ==================== BeanDefinitionRegistry接口实现 ====================
    
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition definition) {
        if (beanName == null || definition == null) {
            throw new IllegalArgumentException("beanName和definition不能为null");
        }
        
        // 允许覆盖（第四阶段需要，因为配置类本身也会被注册）
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
    
    // ==================== 第四阶段新增：按类型获取Bean ====================
    
    /**
     * 按类型获取Bean（第四阶段新增）
     */
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        Map<String, T> beans = getBeansOfType(requiredType);
        
        if (beans.isEmpty()) {
            throw new BeansException("找不到类型为 " + requiredType.getName() + " 的Bean");
        }
        
        if (beans.size() > 1) {
            throw new BeansException(
                "找到多个类型为 " + requiredType.getName() + " 的Bean: " + beans.keySet() +
                "，请使用@Qualifier指定Bean名称"
            );
        }
        
        return beans.values().iterator().next();
    }
    
    /**
     * 按类型获取所有Bean（第四阶段新增）
     */
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        Map<String, T> result = new LinkedHashMap<>();
        
        for (String beanName : getBeanDefinitionNames()) {
            BeanDefinition bd = beanDefinitions.get(beanName);
            
            try {
                // 加载类并检查类型
                Class<?> beanClass = ClassUtils.forName(
                    bd.getBeanClassName(), 
                    ClassUtils.getDefaultClassLoader()
                );
                
                if (type.isAssignableFrom(beanClass)) {
                    T bean = getBean(beanName, type);
                    result.put(beanName, bean);
                }
            } catch (ClassNotFoundException e) {
                // 忽略无法加载的类
            }
        }
        
        return result;
    }
    
    /**
     * 获取所有Bean定义名称（第四阶段新增）
     */
    public String[] getBeanDefinitionNames() {
        return beanDefinitions.keySet().toArray(new String[0]);
    }
    
    // ==================== BeanPostProcessor管理 ====================
    
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
    }
    
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }
    
    // ==================== 容器关闭 ====================
    
    public void close() {
        List<String> beanNames = new ArrayList<>(disposableBeans.keySet());
        Collections.reverse(beanNames);
        
        for (String beanName : beanNames) {
            try {
                destroyBean(beanName);
            } catch (Exception e) {
                System.err.println("销毁Bean失败: " + beanName + ", " + e.getMessage());
            }
        }
        
        disposableBeans.clear();
    }
    
    // ==================== 核心方法（来自v3） ====================
    
    private Object doGetBean(String beanName) {
        if (beanName == null) {
            throw new IllegalArgumentException("Bean name不能为null");
        }
        
        Object sharedInstance = getSingleton(beanName);
        if (sharedInstance != null) {
            return sharedInstance;
        }
        
        BeanDefinition bd = beanDefinitions.get(beanName);
        if (bd == null) {
            throw new BeansException("Bean不存在: " + beanName);
        }
        
        if (bd.isSingleton()) {
            if (bd.hasConstructorArgumentValues() && isSingletonCurrentlyInCreation(beanName)) {
                throw new BeansException("检测到构造器循环依赖，无法解决: " + beanName);
            }
            
            sharedInstance = getSingleton(beanName, () -> {
                return createBean(beanName, bd);
            });
            return sharedInstance;
        }
        
        if (bd.isPrototype()) {
            if (isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeansException("检测到原型Bean的循环依赖，无法解决: " + beanName);
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
    
    private Object getSingleton(String beanName) {
        Object singletonObject = singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                singletonObject = earlySingletonObjects.get(beanName);
                if (singletonObject == null) {
                    ObjectFactory singletonFactory = singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        singletonObject = singletonFactory.getObject();
                        earlySingletonObjects.put(beanName, singletonObject);
                        singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return singletonObject;
    }
    
    private Object getSingleton(String beanName, ObjectFactory objectFactory) {
        synchronized (this.singletonObjects) {
            Object singletonObject = singletonObjects.get(beanName);
            if (singletonObject == null) {
                beforeSingletonCreation(beanName);
                
                try {
                    singletonObject = objectFactory.getObject();
                    earlySingletonObjects.remove(beanName);
                    singletonFactories.remove(beanName);
                    singletonObjects.put(beanName, singletonObject);
                } catch (Exception e) {
                    throw new BeansException("创建Bean失败: " + beanName, e);
                } finally {
                    afterSingletonCreation(beanName);
                }
            }
            return singletonObject;
        }
    }
    
    private Object createBean(String beanName, BeanDefinition bd) {
        Object bean = instantiateBean(bd);
        
        if (bd.isSingleton() && !bd.hasConstructorArgumentValues()) {
            addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, bd, bean));
        }
        
        populateBean(beanName, bean, bd);
        bean = initializeBean(beanName, bean, bd);
        
        return bean;
    }
    
    private Object getEarlyBeanReference(String beanName, BeanDefinition bd, Object bean) {
        return bean;
    }
    
    private Object instantiateBean(BeanDefinition bd) {
        String className = bd.getBeanClassName();
        if (className == null || className.trim().isEmpty()) {
            throw new BeansException("Bean的类名不能为空");
        }
        
        try {
            Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
            
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
            throw new BeansException("无法实例化类: " + className, e);
        } catch (IllegalAccessException e) {
            throw new BeansException("无法访问构造函数: " + className, e);
        } catch (InvocationTargetException e) {
            throw new BeansException("构造函数执行异常: " + className, e.getCause());
        }
    }
    
    private Object instantiateUsingConstructor(Class<?> clazz, ConstructorArgument constructorArg) {
        List<ConstructorArgument.ValueHolder> valueHolders = constructorArg.getArgumentValues();
        List<Object> resolvedArgs = new ArrayList<>();
        
        for (ConstructorArgument.ValueHolder valueHolder : valueHolders) {
            Object resolvedValue = resolveValueIfNecessary(valueHolder.getValue());
            resolvedArgs.add(resolvedValue);
        }
        
        Class<?>[] paramTypes = new Class<?>[resolvedArgs.size()];
        for (int i = 0; i < resolvedArgs.size(); i++) {
            paramTypes[i] = resolvedArgs.get(i).getClass();
        }
        
        try {
            Constructor<?> constructor = findMatchingConstructor(clazz, paramTypes);
            return constructor.newInstance(resolvedArgs.toArray());
        } catch (Exception e) {
            throw new BeansException("使用构造器创建Bean失败: " + clazz.getName(), e);
        }
    }
    
    private Constructor<?> findMatchingConstructor(Class<?> clazz, Class<?>[] paramTypes) 
            throws NoSuchMethodException {
        try {
            return clazz.getConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            // 兼容匹配
        }
        
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
        
        throw new NoSuchMethodException("找不到匹配的构造器");
    }
    
    private boolean isAssignable(Class<?> target, Class<?> source) {
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
    
    private void populateBean(String beanName, Object bean, BeanDefinition bd) {
        PropertyValues pvs = bd.getPropertyValues();
        if (pvs.isEmpty()) {
            return;
        }
        
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
            
            for (PropertyValue pv : pvs.getPropertyValues()) {
                String propertyName = pv.getName();
                Object value = pv.getValue();
                
                Object resolvedValue = resolveValueIfNecessary(value);
                
                PropertyDescriptor pd = findPropertyDescriptor(pds, propertyName);
                if (pd == null || pd.getWriteMethod() == null) {
                    throw new BeansException(
                        "Bean[" + beanName + "]没有属性[" + propertyName + "]的setter方法"
                    );
                }
                
                Class<?> propertyType = pd.getPropertyType();
                Object convertedValue = convertValueIfNecessary(resolvedValue, propertyType);
                
                Method writeMethod = pd.getWriteMethod();
                writeMethod.invoke(bean, convertedValue);
            }
        } catch (Exception e) {
            throw new BeansException("属性注入失败: " + beanName, e);
        }
    }
    
    private PropertyDescriptor findPropertyDescriptor(PropertyDescriptor[] pds, String propertyName) {
        for (PropertyDescriptor pd : pds) {
            if (pd.getName().equals(propertyName)) {
                return pd;
            }
        }
        return null;
    }
    
    private Object resolveValueIfNecessary(Object value) {
        if (value instanceof RuntimeBeanReference) {
            RuntimeBeanReference ref = (RuntimeBeanReference) value;
            return getBean(ref.getBeanName());
        } else if (value instanceof TypedStringValue) {
            return ((TypedStringValue) value).getValue();
        }
        return value;
    }
    
    private Object convertValueIfNecessary(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isInstance(value)) {
            return value;
        }
        
        if (value instanceof String) {
            return typeConverter.convertIfNecessary((String) value, targetType);
        }
        
        return value;
    }
    
    // ==================== 第三阶段：生命周期管理 ====================
    
    private Object initializeBean(String beanName, Object bean, BeanDefinition bd) {
        invokeAwareMethods(beanName, bean);
        
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
        
        try {
            invokeInitMethods(beanName, wrappedBean, bd);
        } catch (Exception e) {
            throw new BeansException("初始化方法调用失败: " + beanName, e);
        }
        
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        
        registerDisposableBeanIfNecessary(beanName, wrappedBean, bd);
        
        return wrappedBean;
    }
    
    private void invokeAwareMethods(String beanName, Object bean) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(this);
        }
    }
    
    private Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) {
        Object result = existingBean;
        
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        
        return result;
    }
    
    private Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) {
        Object result = existingBean;
        
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        
        return result;
    }
    
    private void invokeInitMethods(String beanName, Object bean, BeanDefinition bd) 
            throws Exception {
        
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
        
        String initMethodName = bd.getInitMethodName();
        if (initMethodName != null && !initMethodName.trim().isEmpty()) {
            if (bean instanceof InitializingBean && "afterPropertiesSet".equals(initMethodName)) {
                return;
            }
            
            try {
                Method initMethod = bean.getClass().getMethod(initMethodName);
                initMethod.invoke(bean);
            } catch (NoSuchMethodException e) {
                throw new BeansException("初始化方法不存在: " + initMethodName, e);
            } catch (InvocationTargetException e) {
                throw new BeansException("初始化方法执行失败: " + initMethodName, e.getCause());
            }
        }
    }
    
    private void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition bd) {
        if (!bd.isSingleton()) {
            return;
        }
        
        boolean needsDestroy = (bean instanceof DisposableBean) ||
                              (bd.getDestroyMethodName() != null && 
                               !bd.getDestroyMethodName().trim().isEmpty());
        
        if (needsDestroy) {
            disposableBeans.put(beanName, bean);
        }
    }
    
    private void destroyBean(String beanName) throws Exception {
        Object bean = disposableBeans.get(beanName);
        if (bean == null) {
            return;
        }
        
        BeanDefinition bd = getBeanDefinition(beanName);
        
        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }
        
        String destroyMethodName = bd.getDestroyMethodName();
        if (destroyMethodName != null && !destroyMethodName.trim().isEmpty()) {
            if (bean instanceof DisposableBean && "destroy".equals(destroyMethodName)) {
                return;
            }
            
            try {
                Method destroyMethod = bean.getClass().getMethod(destroyMethodName);
                destroyMethod.invoke(bean);
            } catch (NoSuchMethodException e) {
                throw new BeansException("销毁方法不存在: " + destroyMethodName, e);
            } catch (InvocationTargetException e) {
                throw new BeansException("销毁方法执行失败: " + destroyMethodName, e.getCause());
            }
        }
    }
    
    private void addSingletonFactory(String beanName, ObjectFactory objectFactory) {
        synchronized (this.singletonObjects) {
            if (!singletonObjects.containsKey(beanName)) {
                singletonFactories.put(beanName, objectFactory);
                earlySingletonObjects.remove(beanName);
            }
        }
    }
    
    // ==================== 循环依赖检测 ====================
    
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
    
    private final ThreadLocal<Set<String>> prototypesCurrentlyInCreation = 
        ThreadLocal.withInitial(HashSet::new);
    
    private void beforePrototypeCreation(String beanName) {
        Set<String> creating = prototypesCurrentlyInCreation.get();
        if (!creating.add(beanName)) {
            throw new BeansException("检测到原型Bean的循环依赖: " + beanName);
        }
    }
    
    private void afterPrototypeCreation(String beanName) {
        prototypesCurrentlyInCreation.get().remove(beanName);
    }
    
    private boolean isPrototypeCurrentlyInCreation(String beanName) {
        return prototypesCurrentlyInCreation.get().contains(beanName);
    }
    
    @FunctionalInterface
    public interface ObjectFactory {
        Object getObject() throws BeansException;
    }
}

