package com.litespring.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 资源抽象接口
 * 用于统一访问不同来源的配置文件（classpath、文件系统、URL等）
 * 
 * @author lite-spring
 */
public interface Resource {
    
    /**
     * 获取资源的输入流
     * 
     * @return 输入流
     * @throws IOException 如果资源不存在或无法读取
     */
    InputStream getInputStream() throws IOException;
    
    /**
     * 判断资源是否存在
     * 
     * @return 如果资源存在返回true，否则返回false
     */
    boolean exists();
    
    /**
     * 获取资源描述信息
     * 
     * @return 资源描述
     */
    String getDescription();
}

