package com.litespring.test.v2.dao;

/**
 * 测试用的Dao接口
 * 
 * @author lite-spring
 */
public interface UserDao {
    
    void save(String username);
    
    String findById(int id);
}

