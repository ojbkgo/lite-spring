package com.litespring.core.io;

import com.litespring.util.ClassUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 从classpath加载资源的实现
 * 
 * @author lite-spring
 */
public class ClassPathResource implements Resource {
    
    private final String path;
    private final ClassLoader classLoader;
    
    /**
     * 使用默认类加载器
     */
    public ClassPathResource(String path) {
        this(path, ClassUtils.getDefaultClassLoader());
    }
    
    /**
     * 使用指定的类加载器
     */
    public ClassPathResource(String path, ClassLoader classLoader) {
        if (path == null) {
            throw new IllegalArgumentException("Path不能为null");
        }
        this.path = path;
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is = this.classLoader.getResourceAsStream(this.path);
        if (is == null) {
            throw new FileNotFoundException("资源不存在: " + getDescription());
        }
        return is;
    }
    
    @Override
    public boolean exists() {
        return (this.classLoader.getResource(this.path) != null);
    }
    
    @Override
    public String getDescription() {
        return "class path resource [" + this.path + "]";
    }
    
    /**
     * 获取资源路径
     */
    public String getPath() {
        return this.path;
    }
}

