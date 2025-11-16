package com.litespring.test.v2.service;

import com.litespring.test.v2.dao.UserDao;

/**
 * 测试用的Service类（Setter注入）
 * 
 * @author lite-spring
 */
public class UserService {
    
    private UserDao userDao;
    private int maxRetry;
    private String serviceName;
    
    public UserService() {
        System.out.println("UserService实例被创建");
    }
    
    // Setter方法（用于属性注入）
    public void setUserDao(UserDao userDao) {
        System.out.println("注入UserDao");
        this.userDao = userDao;
    }
    
    public void setMaxRetry(int maxRetry) {
        System.out.println("注入maxRetry: " + maxRetry);
        this.maxRetry = maxRetry;
    }
    
    public void setServiceName(String serviceName) {
        System.out.println("注入serviceName: " + serviceName);
        this.serviceName = serviceName;
    }
    
    // Getter方法
    public UserDao getUserDao() {
        return userDao;
    }
    
    public int getMaxRetry() {
        return maxRetry;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    // 业务方法
    public void saveUser(String username) {
        if (userDao == null) {
            throw new IllegalStateException("UserDao未注入");
        }
        userDao.save(username);
    }
}

