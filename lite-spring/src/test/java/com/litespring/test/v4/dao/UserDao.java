package com.litespring.test.v4.dao;

/**
 * 用户Dao接口
 * 
 * @author lite-spring
 */
public interface UserDao {
    void save(String username);
    String findById(int id);
}

