package com.litespring.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 行映射器接口
 * 将ResultSet的一行映射为Java对象
 * 
 * @author lite-spring
 * @param <T> 映射的对象类型
 */
@FunctionalInterface
public interface RowMapper<T> {
    
    /**
     * 映射一行数据
     * 
     * @param rs ResultSet对象（已定位到当前行）
     * @param rowNum 行号（从0开始）
     * @return 映射的对象
     * @throws SQLException 如果映射失败
     */
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}

