package com.litespring.test.v1;

import com.litespring.core.io.ClassPathResource;
import com.litespring.core.io.Resource;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resource测试类
 * 测试资源加载功能
 * 
 * @author lite-spring
 */
public class ResourceTest {
    
    /**
     * 测试：从classpath加载资源
     */
    @Test
    public void testClassPathResource() throws IOException {
        Resource resource = new ClassPathResource("beans-v1.xml");
        
        // 验证资源存在
        assertTrue(resource.exists());
        
        // 验证能获取输入流
        InputStream is = resource.getInputStream();
        assertNotNull(is);
        
        // 读取一些内容验证
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String firstLine = reader.readLine();
        assertNotNull(firstLine);
        assertTrue(firstLine.contains("<?xml"));
        
        reader.close();
    }
    
    /**
     * 测试：资源不存在时抛出异常
     */
    @Test
    public void testNonExistentResource() {
        Resource resource = new ClassPathResource("nonexistent.xml");
        
        // 资源不存在
        assertFalse(resource.exists());
        
        // 尝试获取输入流应该抛出异常
        assertThrows(IOException.class, () -> {
            resource.getInputStream();
        });
    }
    
    /**
     * 测试：获取资源描述
     */
    @Test
    public void testResourceDescription() {
        Resource resource = new ClassPathResource("beans-v1.xml");
        
        String description = resource.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("beans-v1.xml"));
    }
}

