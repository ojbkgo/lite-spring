package com.litespring.test.v7;

import com.litespring.jdbc.JdbcTemplate;
import com.litespring.jdbc.RowMapper;
import com.litespring.jdbc.datasource.DataSourceConfig;
import com.litespring.jdbc.datasource.HikariDataSourceFactory;
import com.litespring.test.v7.model.User;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JdbcTemplate测试
 * 
 * @author lite-spring
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JdbcTemplateTest {
    
    private static DataSource dataSource;
    private static JdbcTemplate jdbcTemplate;
    
    @BeforeAll
    public static void setUpClass() {
        // 使用H2内存数据库
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(5);
        
        // 创建HikariCP数据源
        dataSource = HikariDataSourceFactory.createDataSource(config);
        
        // 创建JdbcTemplate
        jdbcTemplate = new JdbcTemplate(dataSource);
        
        System.out.println("========== 数据源创建完成 ==========");
        System.out.println("使用HikariCP连接池");
    }
    
    @BeforeEach
    public void setUp() {
        // 创建表
        jdbcTemplate.update("DROP TABLE IF EXISTS users");
        jdbcTemplate.update(
            "CREATE TABLE users (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  name VARCHAR(50)," +
            "  age INT," +
            "  email VARCHAR(100)" +
            ")"
        );
        
        System.out.println("\n========== 创建测试表 ==========");
    }
    
    /**
     * 测试：插入数据
     */
    @Test
    @Order(1)
    public void testInsert() {
        String sql = "INSERT INTO users (name, age, email) VALUES (?, ?, ?)";
        
        int rows = jdbcTemplate.update(sql, "Tom", 25, "tom@example.com");
        
        assertEquals(1, rows);
        System.out.println("插入成功：" + rows + " 行");
    }
    
    /**
     * 测试：查询单个对象
     */
    @Test
    @Order(2)
    public void testQueryForObject() {
        // 插入测试数据
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Alice", 30);
        
        // 查询
        String sql = "SELECT * FROM users WHERE name = ?";
        User user = jdbcTemplate.queryForObject(sql, new UserRowMapper(), "Alice");
        
        assertNotNull(user);
        assertEquals("Alice", user.getName());
        assertEquals(30, user.getAge());
        
        System.out.println("查询结果：" + user);
    }
    
    /**
     * 测试：查询列表
     */
    @Test
    @Order(3)
    public void testQuery() {
        // 插入测试数据
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Bob", 20);
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Charlie", 35);
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "David", 28);
        
        // 查询所有
        String sql = "SELECT * FROM users ORDER BY age";
        List<User> users = jdbcTemplate.query(sql, new UserRowMapper());
        
        assertEquals(3, users.size());
        assertEquals("Bob", users.get(0).getName());
        assertEquals("David", users.get(1).getName());
        assertEquals("Charlie", users.get(2).getName());
        
        System.out.println("查询到 " + users.size() + " 个用户");
        users.forEach(System.out::println);
    }
    
    /**
     * 测试：更新数据
     */
    @Test
    @Order(4)
    public void testUpdate() {
        // 插入
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Eve", 22);
        
        // 更新
        String sql = "UPDATE users SET age = ? WHERE name = ?";
        int rows = jdbcTemplate.update(sql, 23, "Eve");
        
        assertEquals(1, rows);
        
        // 验证
        User user = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE name = ?",
            new UserRowMapper(),
            "Eve"
        );
        assertEquals(23, user.getAge());
        
        System.out.println("更新成功，新年龄：" + user.getAge());
    }
    
    /**
     * 测试：删除数据
     */
    @Test
    @Order(5)
    public void testDelete() {
        // 插入
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Frank", 40);
        
        // 删除
        String sql = "DELETE FROM users WHERE name = ?";
        int rows = jdbcTemplate.update(sql, "Frank");
        
        assertEquals(1, rows);
        
        // 验证
        User user = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE name = ?",
            new UserRowMapper(),
            "Frank"
        );
        assertNull(user);
        
        System.out.println("删除成功");
    }
    
    /**
     * 测试：查询基本类型
     */
    @Test
    @Order(6)
    public void testQueryForPrimitiveType() {
        // 插入测试数据
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Test1", 20);
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Test2", 30);
        
        // 查询数量
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users",
            Integer.class
        );
        
        assertEquals(2, count);
        System.out.println("总数：" + count);
    }
    
    /**
     * 测试：批量更新
     */
    @Test
    @Order(7)
    public void testBatchUpdate() {
        String sql = "INSERT INTO users (name, age) VALUES (?, ?)";
        
        List<Object[]> batchArgs = java.util.Arrays.asList(
            new Object[]{"User1", 21},
            new Object[]{"User2", 22},
            new Object[]{"User3", 23}
        );
        
        int[] rows = jdbcTemplate.batchUpdate(sql, batchArgs);
        
        assertEquals(3, rows.length);
        assertEquals(1, rows[0]);
        assertEquals(1, rows[1]);
        assertEquals(1, rows[2]);
        
        System.out.println("批量插入成功：" + rows.length + " 行");
    }
    
    /**
     * 测试：Lambda表达式作为RowMapper
     */
    @Test
    @Order(8)
    public void testLambdaRowMapper() {
        jdbcTemplate.update("INSERT INTO users (name, age) VALUES (?, ?)", "Lambda", 99);
        
        // 使用Lambda表达式
        User user = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE name = ?",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            },
            "Lambda"
        );
        
        assertNotNull(user);
        assertEquals("Lambda", user.getName());
        
        System.out.println("Lambda RowMapper：" + user);
    }
    
    // ==================== 辅助类 ====================
    
    /**
     * User行映射器
     */
    static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            
            String email = rs.getString("email");
            if (email != null) {
                user.setEmail(email);
            }
            
            return user;
        }
    }
}

