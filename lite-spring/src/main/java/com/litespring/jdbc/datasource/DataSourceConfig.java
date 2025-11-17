package com.litespring.jdbc.datasource;

/**
 * 数据源配置类
 * 封装数据源的配置信息
 * 
 * @author lite-spring
 */
public class DataSourceConfig {
    
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    
    // HikariCP配置
    private int minimumIdle = 2;
    private int maximumPoolSize = 10;
    private long connectionTimeout = 30000;
    private long idleTimeout = 600000;
    private long maxLifetime = 1800000;
    private String connectionTestQuery = "SELECT 1";
    private String poolName = "LiteSpring-Pool";
    
    // ==================== Getter和Setter ====================
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDriverClassName() {
        return driverClassName;
    }
    
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }
    
    public int getMinimumIdle() {
        return minimumIdle;
    }
    
    public void setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
    }
    
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    
    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }
    
    public long getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public long getIdleTimeout() {
        return idleTimeout;
    }
    
    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }
    
    public long getMaxLifetime() {
        return maxLifetime;
    }
    
    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }
    
    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }
    
    public void setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
    }
    
    public String getPoolName() {
        return poolName;
    }
    
    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }
}

