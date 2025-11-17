package com.litespring.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接回调接口
 * 在Connection上执行操作
 * 
 * @author lite-spring
 * @param <T> 返回值类型
 */
@FunctionalInterface
public interface ConnectionCallback<T> {
    
    /**
     * 在Connection上执行操作
     * 
     * @param conn 数据库连接
     * @return 操作结果
     * @throws SQLException 如果操作失败
     */
    T doInConnection(Connection conn) throws SQLException;
}

