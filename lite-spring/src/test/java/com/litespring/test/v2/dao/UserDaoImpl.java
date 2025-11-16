package com.litespring.test.v2.dao;

/**
 * 测试用的Dao实现
 * 
 * @author lite-spring
 */
public class UserDaoImpl implements UserDao {
    
    public UserDaoImpl() {
        System.out.println("UserDaoImpl实例被创建");
    }
    
    @Override
    public void save(String username) {
        System.out.println("保存用户: " + username);
    }
    
    @Override
    public String findById(int id) {
        return "User-" + id;
    }
}

