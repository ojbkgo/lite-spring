package com.litespring.test.v5.service;

/**
 * 用户服务接口
 * 用于测试JDK动态代理（需要接口）
 * 
 * @author lite-spring
 */
public interface UserService {
    
    /**
     * 保存用户
     */
    void saveUser(String username);
    
    /**
     * 查找用户
     */
    String findUser(int id);
    
    /**
     * 删除用户
     */
    void deleteUser(int id);
}

