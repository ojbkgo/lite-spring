package com.litespring.jdbc;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC模板类
 * 简化JDBC操作，自动管理资源
 * 
 * 核心功能：
 * 1. 自动获取和释放Connection
 * 2. 自动关闭Statement和ResultSet
 * 3. 统一的异常处理
 * 4. 简化的API
 * 
 * @author lite-spring
 */
public class JdbcTemplate {
    
    private DataSource dataSource;
    
    public JdbcTemplate() {
    }
    
    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    // ==================== 查询方法 ====================
    
    /**
     * 查询单个对象
     * 
     * @param sql SQL语句
     * @param rowMapper 行映射器
     * @param args 参数
     * @return 查询结果，如果没有返回null
     */
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> results = query(sql, rowMapper, args);
        
        if (results == null || results.isEmpty()) {
            return null;
        }
        
        if (results.size() > 1) {
            throw new DataAccessException(
                "期望返回1个结果，实际返回" + results.size() + "个"
            );
        }
        
        return results.get(0);
    }
    
    /**
     * 查询列表
     * 
     * @param sql SQL语句
     * @param rowMapper 行映射器
     * @param args 参数
     * @return 查询结果列表
     */
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute((ConnectionCallback<List<T>>) conn -> {
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                // 创建PreparedStatement
                ps = conn.prepareStatement(sql);
                
                // 设置参数
                setParameters(ps, args);
                
                // 执行查询
                rs = ps.executeQuery();
                
                // 映射结果
                List<T> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    T obj = rowMapper.mapRow(rs, rowNum++);
                    if (obj != null) {
                        results.add(obj);
                    }
                }
                
                return results;
                
            } finally {
                closeResultSet(rs);
                closeStatement(ps);
            }
        });
    }
    
    /**
     * 查询基本类型
     */
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
        return queryForObject(sql, (rs, rowNum) -> {
            Object value = rs.getObject(1);
            return convertValue(value, requiredType);
        }, args);
    }
    
    /**
     * 查询Map
     */
    public java.util.Map<String, Object> queryForMap(String sql, Object... args) {
        return queryForObject(sql, (rs, rowNum) -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                map.put(columnName, value);
            }
            
            return map;
        }, args);
    }
    
    // ==================== 更新方法 ====================
    
    /**
     * 执行更新（INSERT、UPDATE、DELETE）
     * 
     * @param sql SQL语句
     * @param args 参数
     * @return 影响的行数
     */
    public int update(String sql, Object... args) {
        return execute(conn -> {
            PreparedStatement ps = null;
            
            try {
                ps = conn.prepareStatement(sql);
                setParameters(ps, args);
                return ps.executeUpdate();
                
            } finally {
                closeStatement(ps);
            }
        });
    }
    
    /**
     * 批量更新
     */
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        return execute(conn -> {
            PreparedStatement ps = null;
            
            try {
                ps = conn.prepareStatement(sql);
                
                for (Object[] args : batchArgs) {
                    setParameters(ps, args);
                    ps.addBatch();
                }
                
                return ps.executeBatch();
                
            } finally {
                closeStatement(ps);
            }
        });
    }
    
    // ==================== 核心执行方法 ====================
    
    /**
     * 执行回调（模板方法）
     * 自动管理连接
     */
    public <T> T execute(ConnectionCallback<T> action) {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource未设置");
        }
        
        Connection conn = null;
        
        try {
            // 获取连接
            conn = dataSource.getConnection();
            
            // 执行回调
            return action.doInConnection(conn);
            
        } catch (SQLException e) {
            throw new DataAccessException("数据库操作失败: " + e.getMessage(), e);
        } finally {
            // 释放连接
            releaseConnection(conn);
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 设置PreparedStatement参数
     */
    private void setParameters(PreparedStatement ps, Object... args) throws SQLException {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                
                if (arg == null) {
                    ps.setNull(i + 1, Types.NULL);
                } else if (arg instanceof String) {
                    ps.setString(i + 1, (String) arg);
                } else if (arg instanceof Integer) {
                    ps.setInt(i + 1, (Integer) arg);
                } else if (arg instanceof Long) {
                    ps.setLong(i + 1, (Long) arg);
                } else if (arg instanceof Double) {
                    ps.setDouble(i + 1, (Double) arg);
                } else if (arg instanceof Boolean) {
                    ps.setBoolean(i + 1, (Boolean) arg);
                } else if (arg instanceof java.util.Date) {
                    ps.setTimestamp(i + 1, new Timestamp(((java.util.Date) arg).getTime()));
                } else {
                    // 其他类型使用setObject
                    ps.setObject(i + 1, arg);
                }
            }
        }
    }
    
    /**
     * 类型转换
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> requiredType) {
        if (value == null) {
            return null;
        }
        
        if (requiredType.isInstance(value)) {
            return (T) value;
        }
        
        // 基本类型转换
        if (requiredType == Integer.class || requiredType == int.class) {
            return (T) Integer.valueOf(value.toString());
        } else if (requiredType == Long.class || requiredType == long.class) {
            return (T) Long.valueOf(value.toString());
        } else if (requiredType == String.class) {
            return (T) value.toString();
        }
        
        return (T) value;
    }
    
    /**
     * 关闭ResultSet
     */
    private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // 忽略关闭异常
            }
        }
    }
    
    /**
     * 关闭Statement
     */
    private void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // 忽略关闭异常
            }
        }
    }
    
    /**
     * 释放连接
     */
    private void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // 忽略关闭异常
            }
        }
    }
    
    // ==================== Getter和Setter ====================
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}

