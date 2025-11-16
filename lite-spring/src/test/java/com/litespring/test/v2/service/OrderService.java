package com.litespring.test.v2.service;

import com.litespring.test.v2.dao.UserDao;

/**
 * 测试用的Service类（构造器注入）
 * 
 * @author lite-spring
 */
public class OrderService {
    
    private final UserDao userDao;
    private final int maxSize;
    
    // 构造器注入
    public OrderService(UserDao userDao, int maxSize) {
        System.out.println("OrderService构造器被调用");
        System.out.println("  - userDao: " + userDao);
        System.out.println("  - maxSize: " + maxSize);
        this.userDao = userDao;
        this.maxSize = maxSize;
    }
    
    public UserDao getUserDao() {
        return userDao;
    }
    
    public int getMaxSize() {
        return maxSize;
    }
}

