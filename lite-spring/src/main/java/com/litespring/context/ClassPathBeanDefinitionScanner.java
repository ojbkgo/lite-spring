package com.litespring.context;

import com.litespring.annotation.*;
import com.litespring.core.BeanDefinition;
import com.litespring.core.BeanDefinitionRegistry;
import com.litespring.core.BeansException;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * 类路径Bean定义扫描器
 * 扫描指定包下的所有类，注册标注了@Component的类为Bean
 * 
 * @author lite-spring
 */
public class ClassPathBeanDefinitionScanner {
    
    private final BeanDefinitionRegistry registry;
    
    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * 扫描指定的包
     */
    public void scan(String... basePackages) {
        for (String basePackage : basePackages) {
            doScan(basePackage);
        }
    }
    
    /**
     * 执行扫描
     */
    private void doScan(String basePackage) {
        try {
            // 1. 包名转路径：com.litespring.demo → com/litespring/demo
            String packagePath = basePackage.replace('.', '/');
            
            // 2. 获取包的URL
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(packagePath);
            
            if (resource == null) {
                throw new BeansException("包路径不存在: " + basePackage);
            }
            
            // 3. 获取包下所有类
            File directory = new File(resource.getFile());
            Set<Class<?>> classes = findClasses(directory, basePackage);
            
            // 4. 注册组件
            for (Class<?> clazz : classes) {
                if (isCandidate(clazz)) {
                    registerBean(clazz);
                }
            }
            
        } catch (Exception e) {
            throw new BeansException("扫描包失败: " + basePackage, e);
        }
    }
    
    /**
     * 递归查找目录下的所有类
     */
    private Set<Class<?>> findClasses(File directory, String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        
        if (!directory.exists()) {
            return classes;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子包
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                // 加载类
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    // 忽略无法加载的类
                }
            }
        }
        
        return classes;
    }
    
    /**
     * 判断类是否是候选组件
     */
    private boolean isCandidate(Class<?> clazz) {
        // 不是接口、抽象类、注解、枚举
        if (clazz.isInterface() || 
            clazz.isAnnotation() || 
            clazz.isEnum() ||
            java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        
        // 检查是否有@Component或其衍生注解
        return hasComponentAnnotation(clazz);
    }
    
    /**
     * 检查类是否有@Component注解（包括元注解）
     */
    private boolean hasComponentAnnotation(Class<?> clazz) {
        // 直接检查@Component
        if (clazz.isAnnotationPresent(Component.class)) {
            return true;
        }
        
        // 检查其他注解是否包含@Component元注解
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            // 检查注解的注解（元注解）
            if (annotationType.isAnnotationPresent(Component.class)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 注册Bean
     */
    private void registerBean(Class<?> clazz) {
        // 1. 获取Bean名称
        String beanName = determineBeanName(clazz);
        
        // 2. 创建BeanDefinition
        BeanDefinition bd = new BeanDefinition(clazz.getName());
        
        // 3. 注册
        registry.registerBeanDefinition(beanName, bd);
    }
    
    /**
     * 确定Bean名称
     */
    private String determineBeanName(Class<?> clazz) {
        // 从@Component注解获取
        Component component = clazz.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }
        
        // 从@Service注解获取
        Service service = clazz.getAnnotation(Service.class);
        if (service != null && !service.value().isEmpty()) {
            return service.value();
        }
        
        // 从@Repository注解获取
        Repository repository = clazz.getAnnotation(Repository.class);
        if (repository != null && !repository.value().isEmpty()) {
            return repository.value();
        }
        
        // 从@Controller注解获取
        Controller controller = clazz.getAnnotation(Controller.class);
        if (controller != null && !controller.value().isEmpty()) {
            return controller.value();
        }
        
        // 默认：类名首字母小写
        String className = clazz.getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }
}

