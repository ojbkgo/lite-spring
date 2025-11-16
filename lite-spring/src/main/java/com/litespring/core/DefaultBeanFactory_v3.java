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
 * 默认的Bean工厂实现 - 第三阶段增强版
 * 在第二阶段基础上新增：
 * 1. Bean生命周期管理
 * 2. 初始化方法调用（InitializingBean接口 + init-method）
 * 3. 销毁方法调用（DisposableBean接口 + destroy-method）
 * 4. BeanPostProcessor扩展点
 * 5. Aware接口支持（BeanNameAware、BeanFactoryAware）
 * 
 * @author lite-spring
 */
public class DefaultBeanFactory_v3 implements BeanDefinitionRegistry, BeanFactory {
    
    // ==================== 三级缓存 ====================
    
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    private final Map<String, ObjectFactory> singletonFactories = new HashMap<>();
    
    // ==================== 其他存储 ====================
    
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = 
        Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // 【第三阶段新增】存储BeanPostProcessor
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    
    // 【第三阶段新增】存储需要销毁的Bean
    private final Map<String, Object> disposableBeans = new LinkedHashMap<>();
    
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
    
    // ==================== 第三阶段新增：BeanPostProcessor管理 ====================
    
    /**
     * 添加BeanPostProcessor
     */
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        // 移除已存在的（避免重复）
        this.beanPostProcessors.remove(beanPostProcessor);
        // 添加到列表
        this.beanPostProcessors.add(beanPostProcessor);
    }
    
    /**
     * 获取所有BeanPostProcessor
     */
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }
    
    // ==================== 第三阶段新增：容器关闭 ====================
    
    /**
     * 关闭容器，销毁所有单例Bean
     */
    public void close() {
        // 按注册顺序的逆序销毁（后创建的先销毁）
        List<String> beanNames = new ArrayList<>(disposableBeans.keySet());
        Collections.reverse(beanNames);
        
        for (String beanName : beanNames) {
            try {
                destroyBean(beanName);
            } catch (Exception e) {
                // 销毁失败不应该影响其他Bean的销毁
                System.err.println("销毁Bean失败: " + beanName + ", " + e.getMessage());
            }
        }
        
        disposableBeans.clear();
        singletonObjects.clear();
    }
    
    // ==================== 核心方法 ====================
    
    private Object doGetBean(String beanName) {
        if (beanName == null) {
            throw new IllegalArgumentException("Bean name不能为null");
        }
        
        // 1. 尝试从缓存获取
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
            if (bd.hasConstructorArgumentValues() && isSingletonCurrentlyInCreation(beanName)) {
                throw new BeansException("检测到构造器循环依赖，无法解决: " + beanName);
            }
            
            sharedInstance = getSingleton(beanName, () -> {
                return createBean(beanName, bd);
            });
            return sharedInstance;
        }
        
        // 4. 原型Bean
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
    
    /**
     * 创建Bean实例（第三阶段增强）
     */
    private Object createBean(String beanName, BeanDefinition bd) {
        // 1. 实例化Bean
        Object bean = instantiateBean(bd);
        
        // 2. 单例Bean提前暴露
        if (bd.isSingleton() && !bd.hasConstructorArgumentValues()) {
            addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, bd, bean));
        }
        
        // 3. 属性注入
        populateBean(beanName, bean, bd);
        
        // 【第三阶段新增】4. 初始化Bean
        bean = initializeBean(beanName, bean, bd);
        
        return bean;
    }
    
    /**
     * 获取早期Bean引用（支持AOP代理）
     * 第三阶段：简单返回原始Bean
     * 第五阶段（AOP）：可能返回代理对象
     */
    private Object getEarlyBeanReference(String beanName, BeanDefinition bd, Object bean) {
        Object exposedObject = bean;
        
        // 第五阶段会在这里应用InstantiationAwareBeanPostProcessor
        // 创建AOP代理
        
        return exposedObject;
    }
    
    /**
     * 【第三阶段新增】初始化Bean
     * 执行完整的生命周期回调
     */
    private Object initializeBean(String beanName, Object bean, BeanDefinition bd) {
        // 1. 调用Aware接口
        invokeAwareMethods(beanName, bean);
        
        // 2. BeanPostProcessor前置处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
        
        // 3. 调用初始化方法
        try {
            invokeInitMethods(beanName, wrappedBean, bd);
        } catch (Exception e) {
            throw new BeansException("初始化方法调用失败: " + beanName, e);
        }
        
        // 4. BeanPostProcessor后置处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        
        // 5. 注册销毁回调
        registerDisposableBeanIfNecessary(beanName, wrappedBean, bd);
        
        return wrappedBean;
    }
    
    /**
     * 【第三阶段新增】调用Aware接口
     */
    private void invokeAwareMethods(String beanName, Object bean) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(this);
        }
    }
    
    /**
     * 【第三阶段新增】应用BeanPostProcessor前置处理
     */
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
    
    /**
     * 【第三阶段新增】应用BeanPostProcessor后置处理
     */
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
    
    /**
     * 【第三阶段新增】调用初始化方法
     */
    private void invokeInitMethods(String beanName, Object bean, BeanDefinition bd) 
            throws Exception {
        
        // 1. 先调用InitializingBean接口
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
        
        // 2. 再调用自定义init-method
        String initMethodName = bd.getInitMethodName();
        if (initMethodName != null && !initMethodName.trim().isEmpty()) {
            // 避免重复调用（如果接口方法名和init-method相同）
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
    
    /**
     * 【第三阶段新增】注册销毁回调
     */
    private void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition bd) {
        // 只有单例Bean才需要注册销毁
        if (!bd.isSingleton()) {
            return;
        }
        
        // 判断是否需要销毁回调
        boolean needsDestroy = (bean instanceof DisposableBean) ||
                              (bd.getDestroyMethodName() != null && 
                               !bd.getDestroyMethodName().trim().isEmpty());
        
        if (needsDestroy) {
            disposableBeans.put(beanName, bean);
        }
    }
    
    /**
     * 【第三阶段新增】销毁Bean
     */
    private void destroyBean(String beanName) throws Exception {
        Object bean = disposableBeans.get(beanName);
        if (bean == null) {
            return;
        }
        
        BeanDefinition bd = getBeanDefinition(beanName);
        
        // 1. 先调用DisposableBean接口
        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }
        
        // 2. 再调用自定义destroy-method
        String destroyMethodName = bd.getDestroyMethodName();
        if (destroyMethodName != null && !destroyMethodName.trim().isEmpty()) {
            // 避免重复调用
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
    
    // ==================== 实例化和属性注入（第二阶段的代码） ====================
    
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
            throw new BeansException("无法实例化类（可能是抽象类或接口）: " + className, e);
        } catch (IllegalAccessException e) {
            throw new BeansException("无法访问构造函数: " + className, e);
        } catch (InvocationTargetException e) {
            throw new BeansException("构造函数执行时抛出异常: " + className, e.getCause());
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
        
        throw new NoSuchMethodException(
            "找不到匹配的构造器: " + clazz.getName() + 
            ", 参数类型: " + Arrays.toString(paramTypes)
        );
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
     * 对象工厂接口
     */
    @FunctionalInterface
    public interface ObjectFactory {
        Object getObject() throws BeansException;
    }
}

