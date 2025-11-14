package com.litespring.util;

/**
 * 类相关的工具方法
 * 
 * @author lite-spring
 */
public class ClassUtils {
    
    /**
     * 获取默认的类加载器
     * 优先使用线程上下文类加载器，如果没有则使用当前类的类加载器
     * 
     * @return 类加载器
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // 无法访问线程上下文类加载器，回退到系统类加载器
        }
        
        if (cl == null) {
            // 没有线程上下文类加载器，使用当前类的类加载器
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                // 如果当前类的类加载器也为null（可能是引导类加载器加载的），使用系统类加载器
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // 无法获取系统类加载器
                }
            }
        }
        return cl;
    }
    
    /**
     * 加载指定名称的类
     * 
     * @param className 类的完全限定名
     * @param classLoader 类加载器
     * @return 类对象
     * @throws ClassNotFoundException 如果类不存在
     */
    public static Class<?> forName(String className, ClassLoader classLoader) 
            throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = getDefaultClassLoader();
        }
        return Class.forName(className, true, classLoader);
    }
}

