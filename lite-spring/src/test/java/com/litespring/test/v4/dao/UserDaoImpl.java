package com.litespring.test.v4.dao;

import com.litespring.annotation.Repository;

/**
 * 用户Dao实现
 * 使用@Repository注解
 * 
 * @author lite-spring
 */
@Repository  // 标注为数据访问层组件
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

