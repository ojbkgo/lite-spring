package com.litespring.mybatis;

import com.litespring.core.InitializingBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;

import javax.sql.DataSource;
import java.io.InputStream;

/**
 * SqlSessionFactory工厂Bean
 * 将MyBatis的SqlSessionFactory集成到lite-spring容器
 * 
 * 这个类演示了如何将第三方框架集成到lite-spring
 * 
 * @author lite-spring
 */
public class SqlSessionFactoryBean implements InitializingBean {
    
    private DataSource dataSource;
    private String configLocation;  // mybatis-config.xml位置
    private SqlSessionFactory sqlSessionFactory;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 在Bean初始化后创建SqlSessionFactory
        this.sqlSessionFactory = buildSqlSessionFactory();
    }
    
    /**
     * 构建SqlSessionFactory
     */
    private SqlSessionFactory buildSqlSessionFactory() throws Exception {
        if (configLocation != null) {
            // 从XML配置文件构建
            InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(configLocation);
            
            return new SqlSessionFactoryBuilder().build(inputStream);
            
        } else if (dataSource != null) {
            // 使用编程方式构建
            org.apache.ibatis.session.Configuration configuration = 
                new org.apache.ibatis.session.Configuration();
            
            // 设置数据源
            org.apache.ibatis.mapping.Environment environment = 
                new org.apache.ibatis.mapping.Environment(
                    "development",
                    new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),
                    dataSource
                );
            configuration.setEnvironment(environment);
            
            // 其他配置
            configuration.setMapUnderscoreToCamelCase(true);  // 下划线转驼峰
            
            return new SqlSessionFactoryBuilder().build(configuration);
            
        } else {
            throw new IllegalArgumentException("必须设置dataSource或configLocation");
        }
    }
    
    /**
     * 获取SqlSessionFactory
     * 作为FactoryBean使用
     */
    public SqlSessionFactory getObject() {
        return this.sqlSessionFactory;
    }
    
    // ==================== Getter和Setter ====================
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }
}

