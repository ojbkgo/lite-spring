package com.litespring.test.v4.service;

import com.litespring.annotation.Autowired;
import com.litespring.annotation.Service;
import com.litespring.annotation.Value;
import com.litespring.test.v4.dao.UserDao;

/**
 * 用户服务
 * 使用@Service注解和@Autowired自动装配
 * 
 * @author lite-spring
 */
@Service  // 标注为服务层组件
public class UserService {
    
    @Autowired  // 自动装配UserDao
    private UserDao userDao;
    
    @Value("3")  // 注入值
    private int maxRetry;
    
    @Value("UserService")
    private String serviceName;
    
    public UserService() {
        System.out.println("UserService实例被创建");
    }
    
    public void saveUser(String username) {
        System.out.println("Service: 准备保存用户");
        System.out.println("  - 服务名: " + serviceName);
        System.out.println("  - 最大重试: " + maxRetry);
        
        if (userDao == null) {
            throw new IllegalStateException("UserDao未注入");
        }
        
        userDao.save(username);
    }
    
    // Getter方法（用于测试）
    public UserDao getUserDao() {
        return userDao;
    }
    
    public int getMaxRetry() {
        return maxRetry;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}

