# 第七阶段完成指南

## 🎉 恭喜！第七阶段数据访问层已完成

你的lite-spring框架现在支持：
- ✅ JdbcTemplate简化数据库操作
- ✅ HikariCP高性能连接池集成
- ✅ MyBatis ORM框架集成
- ✅ 完整的第三方库集成方案

---

## 📦 已创建的文件

### JDBC核心类（4个）
1. **JdbcTemplate** - JDBC模板类 ⭐核心
   - 位置：`com.litespring.jdbc.JdbcTemplate`
   - 功能：简化JDBC操作，自动管理资源

2. **RowMapper** - 行映射器接口
   - 位置：`com.litespring.jdbc.RowMapper`
   - 功能：ResultSet → Java对象

3. **ConnectionCallback** - 连接回调接口
   - 位置：`com.litespring.jdbc.ConnectionCallback`
   - 功能：在Connection上执行操作

4. **DataAccessException** - 数据访问异常
   - 位置：`com.litespring.jdbc.DataAccessException`
   - 功能：统一的运行时异常

### HikariCP集成（2个）
5. **HikariDataSourceFactory** - 数据源工厂 ⭐
   - 位置：`com.litespring.jdbc.datasource.HikariDataSourceFactory`
   - 功能：创建HikariCP数据源

6. **DataSourceConfig** - 数据源配置
   - 位置：`com.litespring.jdbc.datasource.DataSourceConfig`
   - 功能：封装数据源配置信息

### MyBatis集成（2个）
7. **SqlSessionFactoryBean** - SqlSessionFactory工厂 ⭐
   - 位置：`com.litespring.mybatis.SqlSessionFactoryBean`
   - 功能：将MyBatis集成到lite-spring

8. **MapperScannerConfigurer** - Mapper扫描器
   - 位置：`com.litespring.mybatis.MapperScannerConfigurer`
   - 功能：扫描和注册Mapper接口

### 测试代码
9. **User** - 实体类
10. **UserMapper** - MyBatis Mapper接口
11. **JdbcTemplateTest** - JdbcTemplate测试（8个测试）
12. **MyBatisIntegrationTest** - MyBatis集成测试（5个测试）

### 文档
13. **phase7-data-access.md** - 实现指南
14. **third-party-integration.md** - 第三方库集成指南 ⭐
15. **phase7-completed-guide.md** - 本文档

---

## 🚀 运行测试

### 测试JdbcTemplate

```bash
cd /Users/ziyuewen/Devspace/myprj/lite-spring

# 运行JdbcTemplate测试
mvn test -Dtest=JdbcTemplateTest
```

**你会看到**：
```
========== 数据源创建完成 ==========
使用HikariCP连接池
✅ HikariCP数据源创建成功

========== 创建测试表 ==========
...
插入成功：1 行
查询结果：User{id=1, name='Alice', age=30, email='null'}
...
```

### 测试MyBatis集成

```bash
mvn test -Dtest=MyBatisIntegrationTest
```

**你会看到**：
```
========== MyBatis集成测试 ==========

✅ HikariCP数据源创建成功
✅ MyBatis SqlSessionFactory创建成功
✅ UserMapper注册成功

========== 测试MyBatis插入 ==========
插入成功，生成ID：1

========== HikariCP + MyBatis 协作演示 ==========
1. 使用HikariCP提供的连接
2. MyBatis执行SQL操作
3. 自动管理连接

总用户数：1

✅ HikariCP和MyBatis完美协作！
```

---

## 📚 核心功能演示

### 1. 使用JdbcTemplate

```java
// 创建数据源和JdbcTemplate
DataSource dataSource = HikariDataSourceFactory.createDataSource(...);
JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

// 插入
jdbcTemplate.update(
    "INSERT INTO users (name, age) VALUES (?, ?)",
    "Tom", 25
);

// 查询单个对象
User user = jdbcTemplate.queryForObject(
    "SELECT * FROM users WHERE id = ?",
    (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setAge(rs.getInt("age"));
        return u;
    },
    1
);

// 查询列表
List<User> users = jdbcTemplate.query(
    "SELECT * FROM users",
    new UserRowMapper()
);

// 更新
jdbcTemplate.update(
    "UPDATE users SET age = ? WHERE id = ?",
    26, 1
);

// 删除
jdbcTemplate.update("DELETE FROM users WHERE id = ?", 1);
```

### 2. 使用HikariCP连接池

```java
// 创建配置
DataSourceConfig config = new DataSourceConfig();
config.setUrl("jdbc:mysql://localhost:3306/mydb");
config.setUsername("root");
config.setPassword("password");

// HikariCP配置
config.setMinimumIdle(5);
config.setMaximumPoolSize(20);
config.setConnectionTimeout(30000);

// 创建数据源
DataSource dataSource = HikariDataSourceFactory.createDataSource(config);

// 使用
Connection conn = dataSource.getConnection();
// ...
conn.close();  // 归还到连接池，不是真正关闭
```

**HikariCP的优势**：
- 🚀 性能极佳（比其他连接池快2-3倍）
- 💪 稳定可靠
- 📦 轻量级（130KB）
- ✅ Spring Boot默认连接池

### 3. 使用MyBatis

```java
// 1. 创建SqlSessionFactory
SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
factoryBean.setDataSource(dataSource);
factoryBean.afterPropertiesSet();
SqlSessionFactory sqlSessionFactory = factoryBean.getObject();

// 2. 注册Mapper
sqlSessionFactory.getConfiguration().addMapper(UserMapper.class);

// 3. 使用Mapper
SqlSession session = sqlSessionFactory.openSession();
try {
    UserMapper mapper = session.getMapper(UserMapper.class);
    
    // 增
    User user = new User("Tom", 25);
    mapper.insert(user);
    
    // 查
    User found = mapper.findById(user.getId());
    
    // 改
    found.setAge(26);
    mapper.update(found);
    
    // 删
    mapper.delete(user.getId());
    
    session.commit();
} finally {
    session.close();
}
```

---

## 🎯 第三方库集成的核心要点

### 要点1：使用标准接口

**javax.sql.DataSource接口**：
```java
// 标准接口
public interface DataSource {
    Connection getConnection() throws SQLException;
}

// HikariCP实现
public class HikariDataSource implements DataSource {
    // HikariCP的实现
}

// Druid实现
public class DruidDataSource implements DataSource {
    // Druid的实现
}

// lite-spring只依赖接口
public class JdbcTemplate {
    private DataSource dataSource;  // 任何实现都可以！
}
```

**好处**：
- 解耦：不依赖具体实现
- 灵活：可以随意替换连接池
- 标准：遵循Java规范

### 要点2：利用生命周期回调

**InitializingBean的应用**：
```java
public class SqlSessionFactoryBean implements InitializingBean {
    
    private DataSource dataSource;  // lite-spring注入
    private SqlSessionFactory sqlSessionFactory;  // 第三方对象
    
    // setter方法（lite-spring调用）
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    // 初始化回调（lite-spring调用）
    @Override
    public void afterPropertiesSet() throws Exception {
        // 在这里创建第三方对象
        this.sqlSessionFactory = createSqlSessionFactory();
    }
    
    // 返回第三方对象
    public SqlSessionFactory getObject() {
        return this.sqlSessionFactory;
    }
}
```

**流程**：
```
lite-spring创建SqlSessionFactoryBean
  ↓
注入DataSource（依赖注入）
  ↓
调用afterPropertiesSet（生命周期回调）
  ↓
创建MyBatis的SqlSessionFactory
  ↓
getObject()返回SqlSessionFactory
  ↓
其他Bean可以注入SqlSessionFactory
```

### 要点3：工厂方法模式

**简化第三方对象创建**：
```java
public class HikariDataSourceFactory {
    
    public static DataSource createDataSource(DataSourceConfig config) {
        // 封装复杂的创建逻辑
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        // ... 更多配置
        
        return new HikariDataSource(hikariConfig);
    }
}

// 使用
@Bean
public DataSource dataSource() {
    return HikariDataSourceFactory.createDataSource(config);
}
```

---

## 💡 实际项目配置建议

### 开发环境配置

```java
@Configuration
@Profile("dev")  // 开发环境（后续可实现@Profile）
public class DevDataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        DataSourceConfig config = new DataSourceConfig();
        
        // H2内存数据库（开发测试用）
        config.setUrl("jdbc:h2:mem:devdb");
        config.setUsername("sa");
        config.setPassword("");
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(5);
        
        return HikariDataSourceFactory.createDataSource(config);
    }
}
```

### 生产环境配置

```java
@Configuration
@Profile("prod")  // 生产环境
public class ProdDataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        DataSourceConfig config = new DataSourceConfig();
        
        // MySQL生产库
        config.setUrl("jdbc:mysql://prod-db.example.com:3306/myapp");
        config.setUsername("app_user");
        config.setPassword(System.getenv("DB_PASSWORD"));  // 从环境变量读取
        
        // 生产环境连接池配置
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(50);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return HikariDataSourceFactory.createDataSource(config);
    }
}
```

---

## 📊 JdbcTemplate vs MyBatis使用场景

### JdbcTemplate适用场景

**✅ 推荐使用**：
- 简单的CRUD操作
- SQL语句简单直接
- 需要完全控制SQL
- 学习成本要求低
- 性能要求极高的场景

**示例**：
```java
// 简单查询
List<User> users = jdbcTemplate.query(
    "SELECT * FROM users WHERE age > ?",
    new UserRowMapper(),
    18
);

// 聚合查询
Integer avgAge = jdbcTemplate.queryForObject(
    "SELECT AVG(age) FROM users",
    Integer.class
);
```

### MyBatis适用场景

**✅ 推荐使用**：
- 复杂的SQL查询
- 动态SQL需求
- 多表关联查询
- 结果映射复杂
- 中大型项目

**示例**：
```xml
<!-- 复杂的动态SQL -->
<select id="findUsers" resultType="User">
    SELECT u.*, d.dept_name
    FROM users u
    LEFT JOIN departments d ON u.dept_id = d.id
    <where>
        <if test="name != null">
            AND u.name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="minAge != null">
            AND u.age >= #{minAge}
        </if>
        <if test="deptId != null">
            AND u.dept_id = #{deptId}
        </if>
    </where>
    ORDER BY u.id
    LIMIT #{offset}, #{limit}
</select>
```

### 混合使用（推荐）

```java
@Repository
public class UserDaoImpl {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;  // 简单操作
    
    @Autowired
    private UserMapper userMapper;  // 复杂操作
    
    // 简单查询用JdbcTemplate
    public int countUsers() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users",
            Integer.class
        );
    }
    
    // 复杂查询用MyBatis
    public List<User> searchUsers(UserSearchCriteria criteria) {
        return userMapper.searchUsers(criteria);
    }
}
```

---

## 🎓 学习要点

### 1. JdbcTemplate的模板方法模式

**核心方法**：`execute(ConnectionCallback<T> action)`

```java
public <T> T execute(ConnectionCallback<T> action) {
    Connection conn = null;
    try {
        // 【固定部分】获取连接
        conn = dataSource.getConnection();
        
        // 【可变部分】执行回调
        return action.doInConnection(conn);
        
    } catch (SQLException e) {
        // 【固定部分】异常处理
        throw new DataAccessException("操作失败", e);
    } finally {
        // 【固定部分】释放连接
        releaseConnection(conn);
    }
}
```

**模板方法模式**：
- 固定部分：连接管理、异常处理、资源释放
- 可变部分：具体的SQL操作（通过回调）

### 2. HikariCP连接池原理

**连接池的作用**：
```
不使用连接池：
每次操作 → 创建Connection → 使用 → 关闭 → 销毁
         ↑ 耗时！          ↑ 耗时！

使用连接池：
第一次 → 创建Connection → 放入池中
后续操作 → 从池中获取 → 使用 → 归还到池中（不销毁）
        ↑ 快！               ↑ 复用！
```

**HikariCP特点**：
- 字节码级优化
- 最小化锁竞争
- 连接状态快速切换
- 优化的数据结构

### 3. MyBatis集成原理

**核心**：SqlSessionFactory是MyBatis的入口

```java
// MyBatis的使用流程
SqlSessionFactory factory = ...
  ↓
SqlSession session = factory.openSession()
  ↓
UserMapper mapper = session.getMapper(UserMapper.class)
  ↓
User user = mapper.findById(1)
```

**集成到lite-spring**：
- SqlSessionFactory注册为Bean
- Mapper通过工厂Bean注册
- 可以@Autowired注入使用

---

## 🎯 完整的使用示例

### 在lite-spring中配置数据访问

```java
@Configuration
@ComponentScan("com.litespring.demo")
public class DataConfig {
    
    /**
     * 配置数据源（HikariCP）
     */
    @Bean
    public DataSource dataSource() {
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl("jdbc:h2:mem:demodb");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);
        
        return HikariDataSourceFactory.createDataSource(config);
    }
    
    /**
     * 配置JdbcTemplate
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * 配置MyBatis（可选）
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) 
            throws Exception {
        
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.afterPropertiesSet();
        
        SqlSessionFactory factory = bean.getObject();
        
        // 注册Mapper
        factory.getConfiguration().addMapper(UserMapper.class);
        
        return factory;
    }
}
```

### Dao层实现

```java
@Repository
public class UserDaoImpl implements UserDao {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
    }
    
    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    
    @Override
    public void save(User user) {
        String sql = "INSERT INTO users (name, age, email) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getEmail());
    }
    
    @Override
    public void update(User user) {
        String sql = "UPDATE users SET name = ?, age = ?, email = ? WHERE id = ?";
        jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getEmail(), user.getId());
    }
    
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    // RowMapper
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            user.setEmail(rs.getString("email"));
            return user;
        }
    }
}
```

---

## ✅ 完成清单

- [ ] JdbcTemplate基础功能实现
- [ ] query和update方法正常工作
- [ ] RowMapper正确映射结果
- [ ] HikariCP数据源成功创建
- [ ] 连接池正常工作
- [ ] MyBatis SqlSessionFactory创建成功
- [ ] Mapper接口能正常使用
- [ ] 所有测试通过

---

## 📈 与前几阶段的对比

| 功能 | 阶段1-5 | 阶段7 |
|------|---------|-------|
| **核心** | IoC + AOP | 数据访问 |
| **技术** | 反射、代理 | JDBC |
| **集成** | 自研 | 第三方库 |
| **难度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 🎊 重要成就

### 你现在掌握了

**1. JdbcTemplate使用**
- 模板方法模式的实际应用
- JDBC资源管理
- 异常转换

**2. 连接池集成**
- HikariCP的配置和使用
- 连接池的作用和原理
- 工业级组件的集成

**3. MyBatis集成**
- ORM框架的集成方式
- FactoryBean模式
- Mapper代理机制

**4. 第三方库集成方法**
- 面向接口编程
- 生命周期集成
- 依赖注入应用

---

## 🚀 下一步：事务管理

**第八阶段预告**：
- @Transactional注解
- 基于AOP的事务拦截
- 事务传播机制
- 与数据访问层结合

**为什么重要**：
- 事务是企业应用必备
- AOP的实际应用
- Spring的核心功能
- 面试高频考点

---

## 💬 现在可以

### 1. 运行测试
```bash
mvn test -Dtest="com.litespring.test.v7.*"
```

### 2. 学习代码（2-3小时）
- 阅读JdbcTemplate实现
- 阅读HikariDataSourceFactory
- 阅读SqlSessionFactoryBean
- 理解集成原理

### 3. 查看集成文档
打开 `docs/third-party-integration.md`，详细了解集成方案

### 4. 准备第八阶段
```
"我完成第七阶段了，开始事务管理"
```

---

恭喜完成第七阶段！你不仅实现了数据访问层，还学会了如何集成第三方库！💪🚀

这是非常实用的技能，在实际项目中经常用到！

