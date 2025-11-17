# 第七阶段：数据访问层实现指南

## 🎯 阶段目标

实现JdbcTemplate，简化数据库操作，支持：
- JdbcTemplate模板类
- RowMapper行映射器
- 数据库连接管理
- 异常转换
- 命名参数支持（可选）
- 简单的Repository抽象（可选）

完成后，你将能够：
```java
// 不再写繁琐的JDBC代码
@Repository
public class UserDaoImpl {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
    }
    
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    
    public void save(User user) {
        String sql = "INSERT INTO users (name, age) VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getName(), user.getAge());
    }
}

// RowMapper
class UserRowMapper implements RowMapper<User> {
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setAge(rs.getInt("age"));
        return user;
    }
}
```

---

## 📚 理论基础

### 传统JDBC的问题

**传统JDBC代码**：
```java
public User findById(int id) {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    
    try {
        // 1. 获取连接
        conn = dataSource.getConnection();
        
        // 2. 创建Statement
        ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
        ps.setInt(1, id);
        
        // 3. 执行查询
        rs = ps.executeQuery();
        
        // 4. 处理结果
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            return user;
        }
        
        return null;
        
    } catch (SQLException e) {
        throw new RuntimeException(e);
    } finally {
        // 5. 关闭资源（繁琐！）
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            // 忽略
        }
    }
}
```

**问题**：
- ❌ 代码重复（连接管理、异常处理、资源关闭）
- ❌ 容易出错（忘记关闭资源导致连接泄漏）
- ❌ 异常处理繁琐
- ❌ 业务逻辑被淹没

### JdbcTemplate的优势

**使用JdbcTemplate**：
```java
public User findById(int id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    return jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
}
```

**优势**：
- ✅ 代码简洁（3行代替50行）
- ✅ 自动管理连接
- ✅ 自动关闭资源
- ✅ 统一的异常处理
- ✅ 业务逻辑清晰

### JdbcTemplate的原理

**核心思想**：模板方法模式

```java
// JdbcTemplate封装了固定流程
public <T> T execute(ConnectionCallback<T> action) {
    Connection conn = null;
    try {
        // 1. 获取连接（固定）
        conn = dataSource.getConnection();
        
        // 2. 执行回调（可变）
        return action.doInConnection(conn);
        
    } catch (SQLException e) {
        // 3. 异常处理（固定）
        throw translateException(e);
    } finally {
        // 4. 关闭连接（固定）
        releaseConnection(conn);
    }
}

// 用户只需要提供变化的部分
jdbcTemplate.execute(conn -> {
    // 只写业务逻辑
    PreparedStatement ps = conn.prepareStatement(sql);
    return ps.executeQuery();
});
```

---

## 🏗️ 核心组件设计

### 1. DataSource - 数据源

**作用**：提供数据库连接

```java
// Java标准接口（javax.sql.DataSource）
public interface DataSource {
    Connection getConnection() throws SQLException;
}
```

**实现**：
- 简单实现：DriverManagerDataSource
- 连接池实现：HikariCP、Druid等（第三方）

```java
public class DriverManagerDataSource implements DataSource {
    
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    
    @Override
    public Connection getConnection() throws SQLException {
        // 加载驱动
        Class.forName(driverClassName);
        
        // 获取连接
        return DriverManager.getConnection(url, username, password);
    }
}
```

### 2. JdbcTemplate - JDBC模板

**作用**：简化JDBC操作的核心类

**核心方法**：

```java
public class JdbcTemplate {
    
    private DataSource dataSource;
    
    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 查询单个对象
     */
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> results = query(sql, rowMapper, args);
        
        if (results.isEmpty()) {
            return null;
        }
        
        if (results.size() > 1) {
            throw new DataAccessException("期望1个结果，实际" + results.size() + "个");
        }
        
        return results.get(0);
    }
    
    /**
     * 查询列表
     */
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(conn -> {
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                // 创建Statement
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
                    results.add(obj);
                }
                
                return results;
                
            } finally {
                closeResultSet(rs);
                closeStatement(ps);
            }
        });
    }
    
    /**
     * 更新（INSERT/UPDATE/DELETE）
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
     * 执行回调
     */
    private <T> T execute(ConnectionCallback<T> action) {
        Connection conn = null;
        
        try {
            // 获取连接
            conn = dataSource.getConnection();
            
            // 执行回调
            return action.doInConnection(conn);
            
        } catch (SQLException e) {
            throw new DataAccessException("数据库操作失败", e);
        } finally {
            // 关闭连接
            releaseConnection(conn);
        }
    }
    
    /**
     * 设置参数
     */
    private void setParameters(PreparedStatement ps, Object... args) throws SQLException {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
        }
    }
}
```

### 3. RowMapper - 行映射器

**作用**：将ResultSet的一行映射为Java对象

```java
@FunctionalInterface
public interface RowMapper<T> {
    /**
     * 映射一行数据
     * 
     * @param rs ResultSet对象（已定位到当前行）
     * @param rowNum 行号（从0开始）
     * @return 映射的对象
     */
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
```

**使用示例**：
```java
// 方式1：匿名类
RowMapper<User> mapper = new RowMapper<User>() {
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        return user;
    }
};

// 方式2：Lambda表达式
RowMapper<User> mapper = (rs, rowNum) -> {
    User user = new User();
    user.setId(rs.getInt("id"));
    user.setName(rs.getString("name"));
    return user;
};

// 方式3：独立的类
public class UserRowMapper implements RowMapper<User> {
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setAge(rs.getInt("age"));
        return user;
    }
}
```

### 4. ConnectionCallback - 连接回调

**作用**：封装需要Connection的操作

```java
@FunctionalInterface
public interface ConnectionCallback<T> {
    /**
     * 在连接上执行操作
     */
    T doInConnection(Connection conn) throws SQLException;
}
```

### 5. DataAccessException - 数据访问异常

**作用**：统一的运行时异常

```java
public class DataAccessException extends RuntimeException {
    
    public DataAccessException(String message) {
        super(message);
    }
    
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**为什么需要？**
- JDBC的SQLException是检查异常，使用不便
- 统一转换为运行时异常
- 提供更友好的异常信息

---

## 📋 实现步骤

### 步骤1：创建基础接口

**任务**：定义核心接口
1. `DataSource` - 数据源（使用javax.sql.DataSource）
2. `RowMapper<T>` - 行映射器
3. `ConnectionCallback<T>` - 连接回调
4. `DataAccessException` - 数据访问异常

### 步骤2：实现JdbcTemplate

**任务**：实现核心的JdbcTemplate类

**核心方法**：
```java
public class JdbcTemplate {
    
    // 查询方法
    <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args);
    <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args);
    
    // 更新方法
    int update(String sql, Object... args);
    
    // 批量更新
    int[] batchUpdate(String sql, List<Object[]> batchArgs);
    
    // 执行回调
    <T> T execute(ConnectionCallback<T> action);
}
```

**实现关键点**：
- 模板方法模式
- 资源管理（Connection、Statement、ResultSet）
- 异常转换
- 参数设置

### 步骤3：实现DataSource

**任务**：实现简单的数据源

**简化方案**：
```java
public class SimpleDataSource implements DataSource {
    
    private String url;
    private String username;
    private String password;
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
```

**生产方案**（使用连接池）：
```java
// 集成第三方连接池
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

HikariDataSource dataSource = new HikariDataSource();
dataSource.setJdbcUrl(url);
dataSource.setUsername(username);
dataSource.setPassword(password);
```

### 步骤4：测试

**准备**：
- 内存数据库（H2或SQLite）
- 不需要安装MySQL

**示例**：
```java
@Test
public void testJdbcTemplate() {
    // 1. 创建数据源
    SimpleDataSource dataSource = new SimpleDataSource();
    dataSource.setUrl("jdbc:h2:mem:testdb");
    
    // 2. 创建JdbcTemplate
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    
    // 3. 创建表
    jdbcTemplate.update("CREATE TABLE users (id INT, name VARCHAR(50))");
    
    // 4. 插入数据
    jdbcTemplate.update("INSERT INTO users VALUES (?, ?)", 1, "Tom");
    
    // 5. 查询数据
    List<User> users = jdbcTemplate.query(
        "SELECT * FROM users",
        (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            return user;
        }
    );
    
    assertEquals(1, users.size());
    assertEquals("Tom", users.get(0).getName());
}
```

---

## 🤔 关键难点

### 难点1：资源管理

**问题**：必须确保连接、Statement、ResultSet被关闭

**解决**：
```java
Connection conn = null;
PreparedStatement ps = null;
ResultSet rs = null;

try {
    conn = dataSource.getConnection();
    ps = conn.prepareStatement(sql);
    rs = ps.executeQuery();
    
    // 处理结果
    
} finally {
    // 按顺序关闭（先ResultSet，再Statement，最后Connection）
    closeResultSet(rs);
    closeStatement(ps);
    releaseConnection(conn);
}

private void closeResultSet(ResultSet rs) {
    if (rs != null) {
        try {
            rs.close();
        } catch (SQLException e) {
            // 忽略关闭异常
        }
    }
}
```

### 难点2：异常转换

**问题**：SQLException是检查异常，不便使用

**解决**：
```java
try {
    // JDBC操作
} catch (SQLException e) {
    throw new DataAccessException("操作失败", e);
}
```

### 难点3：参数设置

**问题**：如何优雅地设置PreparedStatement参数？

**解决**：
```java
private void setParameters(PreparedStatement ps, Object... args) 
        throws SQLException {
    if (args != null) {
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            
            if (arg == null) {
                ps.setNull(i + 1, Types.NULL);
            } else {
                ps.setObject(i + 1, arg);
            }
        }
    }
}
```

---

## 📊 与第六阶段的对比

| 方面 | 第六阶段（MVC） | 第七阶段（数据访问） |
|------|----------------|-------------------|
| **关注点** | Web请求处理 | 数据库操作 |
| **核心技术** | Servlet | JDBC |
| **主要类** | DispatcherServlet | JdbcTemplate |
| **依赖** | Servlet容器 | 数据库 |
| **难度** | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **实用性** | 高 | 高 |

---

## ✅ 完成标志

完成第七阶段后，你应该能够：

1. ✅ 使用JdbcTemplate查询数据
2. ✅ 使用JdbcTemplate更新数据
3. ✅ 使用RowMapper映射结果
4. ✅ 自动管理数据库连接
5. ✅ 统一的异常处理
6. ✅ 简洁的数据访问代码

---

## 🎓 学习建议

### 前置知识

**必须掌握**：
- JDBC基础（Connection、Statement、ResultSet）
- SQL语法

**可选了解**：
- 数据库连接池原理
- 事务基础

### 实现顺序

1. DataAccessException
2. ConnectionCallback和RowMapper接口
3. 简单的DataSource
4. JdbcTemplate核心方法
5. 测试验证

### 预计时间

- 理解文档：30分钟
- 实现代码：3-4小时
- 测试调试：1-2小时
- **总计：5-7小时**

---

## 💡 实现建议

### 简化方案（推荐）

**第七阶段可以非常简单**：
- 只实现JdbcTemplate的基础功能
- 使用H2内存数据库测试
- 不实现连接池（使用简单DataSource）
- 为第八阶段事务做准备

### 为什么实现数据访问层？

**原因**：
- 为第八阶段事务管理做准备
- 事务需要基于数据库操作
- JdbcTemplate是事务的基础

---

## 🚀 实现价值

### 学习价值：⭐⭐⭐

**收获**：
- 理解模板方法模式
- 掌握资源管理
- 了解JDBC封装

### 面试价值：⭐⭐⭐

**可能被问**：
- JdbcTemplate的原理？
- 如何管理数据库连接？
- 模板方法模式的应用？

### 实用价值：⭐⭐⭐⭐

**实际开发**：
- 简化数据库操作
- 为事务管理准备
- 企业应用必备

---

## 📝 更好的选择

### 考虑：第八阶段事务管理

**如果时间有限，建议**：
- 简化第七阶段（只实现基础JdbcTemplate）
- 重点放在第八阶段（事务管理）

**原因**：
- 事务是AOP的实际应用
- 事务管理更有学习价值
- 面试常考（@Transactional原理）

---

## 💬 你的选择

### 选项1：简化实现数据访问
```
"实现简化版JdbcTemplate"
```
→ 基础功能，3-4小时

### 选项2：完整实现
```
"完整实现数据访问层"
```
→ 所有功能，5-7小时

### 选项3：跳到事务管理
```
"跳过数据访问，直接实现事务"
```
→ 事务更重要，6-8小时

### 选项4：结束核心开发
```
"我想总结项目了"
```
→ 归档项目，准备展示

---

## 📈 进度提醒

你已经完成：
- ✅ IoC容器
- ✅ 依赖注入  
- ✅ Bean生命周期
- ✅ 注解驱动
- ✅ AOP

**核心功能完成度：90%**

剩余可选模块：
- MVC框架（已跳过）
- 数据访问（当前）
- 事务管理（推荐）

---

告诉我你的选择！🚀

