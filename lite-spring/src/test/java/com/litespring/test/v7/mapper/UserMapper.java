package com.litespring.test.v7.mapper;

import com.litespring.test.v7.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * User Mapper接口
 * 演示MyBatis注解方式
 * 
 * @author lite-spring
 */
public interface UserMapper {
    
    /**
     * 根据ID查找
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(@Param("id") int id);
    
    /**
     * 查找所有用户
     */
    @Select("SELECT * FROM users ORDER BY id")
    List<User> findAll();
    
    /**
     * 根据名称查找
     */
    @Select("SELECT * FROM users WHERE name = #{name}")
    User findByName(@Param("name") String name);
    
    /**
     * 插入用户
     */
    @Insert("INSERT INTO users (name, age, email) VALUES (#{name}, #{age}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    /**
     * 更新用户
     */
    @Update("UPDATE users SET name = #{name}, age = #{age}, email = #{email} WHERE id = #{id}")
    int update(User user);
    
    /**
     * 删除用户
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(int id);
    
    /**
     * 统计数量
     */
    @Select("SELECT COUNT(*) FROM users")
    int count();
}

