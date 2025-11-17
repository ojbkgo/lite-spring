package com.litespring.test.v5.service;

/**
 * 用户服务实现
 * 
 * @author lite-spring
 */
public class UserServiceImpl implements UserService {
    
    @Override
    public void saveUser(String username) {
        System.out.println("【目标方法】保存用户: " + username);
    }
    
    @Override
    public String findUser(int id) {
        System.out.println("【目标方法】查找用户: " + id);
        return "User-" + id;
    }
    
    @Override
    public void deleteUser(int id) {
        System.out.println("【目标方法】删除用户: " + id);
    }
}

