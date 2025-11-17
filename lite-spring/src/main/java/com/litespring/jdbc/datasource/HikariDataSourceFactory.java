package com.litespring.jdbc.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * HikariCP数据源工厂
 * 演示如何集成第三方连接池
 * 
 * HikariCP是目前性能最好的JDBC连接池
 * Spring Boot 2.x默认使用HikariCP
 * 
 * @author lite-spring
 */
public class HikariDataSourceFactory {
    
    /**
     * 创建HikariCP数据源
     * 
     * @param url JDBC URL
     * @param username 用户名
     * @param password 密码
     * @return DataSource
     */
    public static DataSource createDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        
        // 基本配置
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        
        // 连接池配置
        config.setMinimumIdle(2);              // 最小空闲连接数
        config.setMaximumPoolSize(10);         // 最大连接数
        config.setConnectionTimeout(30000);    // 连接超时（30秒）
        config.setIdleTimeout(600000);         // 空闲超时（10分钟）
        config.setMaxLifetime(1800000);        // 连接最大存活时间（30分钟）
        
        // 连接测试
        config.setConnectionTestQuery("SELECT 1");
        
        // 池名称
        config.setPoolName("LiteSpring-HikariCP");
        
        // 性能优化
        config.setAutoCommit(true);
        config.setReadOnly(false);
        
        // 创建数据源
        return new HikariDataSource(config);
    }
    
    /**
     * 创建HikariCP数据源（完整配置）
     */
    public static DataSource createDataSource(DataSourceConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        // 基本配置
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        
        // 驱动类（可选，HikariCP会自动识别）
        if (config.getDriverClassName() != null) {
            hikariConfig.setDriverClassName(config.getDriverClassName());
        }
        
        // 连接池配置
        if (config.getMinimumIdle() > 0) {
            hikariConfig.setMinimumIdle(config.getMinimumIdle());
        }
        if (config.getMaximumPoolSize() > 0) {
            hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        }
        if (config.getConnectionTimeout() > 0) {
            hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        }
        
        // 连接测试
        if (config.getConnectionTestQuery() != null) {
            hikariConfig.setConnectionTestQuery(config.getConnectionTestQuery());
        }
        
        // 池名称
        hikariConfig.setPoolName(config.getPoolName());
        
        return new HikariDataSource(hikariConfig);
    }
}

