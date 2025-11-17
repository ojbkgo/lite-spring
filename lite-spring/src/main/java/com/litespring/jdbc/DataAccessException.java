package com.litespring.jdbc;

/**
 * 数据访问异常
 * 统一的运行时异常，封装SQLException
 * 
 * @author lite-spring
 */
public class DataAccessException extends RuntimeException {
    
    public DataAccessException(String message) {
        super(message);
    }
    
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

