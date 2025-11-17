package com.litespring.test.v7;

import com.litespring.jdbc.datasource.DataSourceConfig;
import com.litespring.jdbc.datasource.HikariDataSourceFactory;
import com.litespring.mybatis.SqlSessionFactoryBean;
import com.litespring.test.v7.mapper.UserMapper;
import com.litespring.test.v7.model.User;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatis集成测试
 * 演示如何在lite-spring中使用MyBatis
 * 
 * @author lite-spring
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MyBatisIntegrationTest {
    
    private static DataSource dataSource;
    private static SqlSessionFactory sqlSessionFactory;
    
    @BeforeAll
    public static void setUpClass() throws Exception {
        System.out.println("\n========== MyBatis集成测试 ==========\n");
        
        // 1. 创建HikariCP数据源
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl("jdbc:h2:mem:mybatisdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(5);
        
        dataSource = HikariDataSourceFactory.createDataSource(config);
        System.out.println("✅ HikariCP数据源创建成功");
        
        // 2. 创建SqlSessionFactory
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.afterPropertiesSet();
        
        sqlSessionFactory = factoryBean.getObject();
        System.out.println("✅ MyBatis SqlSessionFactory创建成功");
        
        // 3. 注册Mapper
        sqlSessionFactory.getConfiguration().addMapper(UserMapper.class);
        System.out.println("✅ UserMapper注册成功\n");
    }
    
    @BeforeEach
    public void setUp() {
        // 每个测试前重建表
        SqlSession session = sqlSessionFactory.openSession();
        try {
            session.getConnection().createStatement().execute("DROP TABLE IF EXISTS users");
            session.getConnection().createStatement().execute(
                "CREATE TABLE users (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(50)," +
                "  age INT," +
                "  email VARCHAR(100)" +
                ")"
            );
            session.commit();
        } catch (Exception e) {
            session.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
    }
    
    /**
     * 测试：MyBatis插入
     */
    @Test
    @Order(1)
    public void testMyBatisInsert() {
        System.out.println("========== 测试MyBatis插入 ==========");
        
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            User user = new User("Tom", 25);
            user.setEmail("tom@example.com");
            
            int rows = mapper.insert(user);
            session.commit();
            
            assertEquals(1, rows);
            assertNotNull(user.getId());  // 自动生成的ID
            
            System.out.println("插入成功，生成ID：" + user.getId());
            
        } finally {
            session.close();
        }
    }
    
    /**
     * 测试：MyBatis查询
     */
    @Test
    @Order(2)
    public void testMyBatisQuery() {
        System.out.println("========== 测试MyBatis查询 ==========");
        
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 插入测试数据
            User user = new User("Alice", 30);
            mapper.insert(user);
            session.commit();
            
            // 查询
            User found = mapper.findById(user.getId());
            
            assertNotNull(found);
            assertEquals("Alice", found.getName());
            assertEquals(30, found.getAge());
            
            System.out.println("查询结果：" + found);
            
        } finally {
            session.close();
        }
    }
    
    /**
     * 测试：MyBatis查询列表
     */
    @Test
    @Order(3)
    public void testMyBatisFindAll() {
        System.out.println("========== 测试MyBatis查询列表 ==========");
        
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 插入多条数据
            mapper.insert(new User("User1", 21));
            mapper.insert(new User("User2", 22));
            mapper.insert(new User("User3", 23));
            session.commit();
            
            // 查询所有
            List<User> users = mapper.findAll();
            
            assertEquals(3, users.size());
            
            System.out.println("查询到 " + users.size() + " 个用户");
            users.forEach(System.out::println);
            
        } finally {
            session.close();
        }
    }
    
    /**
     * 测试：MyBatis更新
     */
    @Test
    @Order(4)
    public void testMyBatisUpdate() {
        System.out.println("========== 测试MyBatis更新 ==========");
        
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 插入
            User user = new User("Bob", 25);
            mapper.insert(user);
            session.commit();
            
            // 更新
            user.setAge(26);
            user.setEmail("bob@example.com");
            int rows = mapper.update(user);
            session.commit();
            
            assertEquals(1, rows);
            
            // 验证
            User updated = mapper.findById(user.getId());
            assertEquals(26, updated.getAge());
            assertEquals("bob@example.com", updated.getEmail());
            
            System.out.println("更新成功：" + updated);
            
        } finally {
            session.close();
        }
    }
    
    /**
     * 测试：展示HikariCP和MyBatis协作
     */
    @Test
    @Order(5)
    public void testHikariCPWithMyBatis() {
        System.out.println("\n========== HikariCP + MyBatis 协作演示 ==========\n");
        
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            System.out.println("1. 使用HikariCP提供的连接");
            System.out.println("2. MyBatis执行SQL操作");
            System.out.println("3. 自动管理连接");
            
            // 插入
            mapper.insert(new User("Integration", 100));
            session.commit();
            
            // 查询
            int count = mapper.count();
            System.out.println("\n总用户数：" + count);
            
            System.out.println("\n✅ HikariCP和MyBatis完美协作！");
            
        } finally {
            session.close();
        }
    }
}

