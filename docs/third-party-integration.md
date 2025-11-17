# 第三方库集成指南

本文档详细说明如何在lite-spring框架中集成第三方库，以HikariCP和MyBatis为例。

---

## 🎯 为什么要集成第三方库？

**原则**：不要重复造轮子

**优秀的第三方库**：
- HikariCP：性能最好的连接池
- MyBatis：优秀的ORM框架
- Jackson：JSON处理
- Logback：日志框架

**集成的好处**：
- 利用成熟的解决方案
- 提高开发效率
- 学习工业级实现

---

## 📦 1. HikariCP连接池集成

### 1.1 为什么选择HikariCP？

**HikariCP特点**：
- 🚀 性能最好（比其他连接池快几倍）
- 💪 稳定可靠
- 📦 体积小（130KB）
- ✅ Spring Boot 2.x默认连接池

### 1.2 添加依赖

**pom.xml**：
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

### 1.3 使用方式

#### 方式1：通过工厂类创建

```java
// 使用HikariDataSourceFactory
DataSource dataSource = HikariDataSourceFactory.createDataSource(
    "jdbc:mysql://localhost:3306/mydb",
    "root",
    "password"
);

// 创建JdbcTemplate
JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
```

#### 方式2：在lite-spring中配置为Bean

**Java配置**：
```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("root");
        config.setPassword("password");
        config.setMaximumPoolSize(10);
        
        return HikariDataSourceFactory.createDataSource(config);
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

**XML配置**：
```xml
<bean id="dataSourceConfig" class="com.litespring.jdbc.datasource.DataSourceConfig">
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="password"/>
    <property name="maximumPoolSize" value="10"/>
</bean>

<bean id="dataSource" 
      class="com.litespring.jdbc.datasource.HikariDataSourceFactory"
      factory-method="createDataSource">
    <constructor-arg ref="dataSourceConfig"/>
</bean>

<bean id="jdbcTemplate" class="com.litespring.jdbc.JdbcTemplate">
    <constructor-arg ref="dataSource"/>
</bean>
```

### 1.4 HikariCP关键配置

```java
HikariConfig config = new HikariConfig();

// ========== 必需配置 ==========
config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
config.setUsername("root");
config.setPassword("password");

// ========== 连接池大小 ==========
config.setMinimumIdle(5);          // 最小空闲连接数（建议：5-10）
config.setMaximumPoolSize(20);     // 最大连接数（建议：CPU核心数 * 2）

// ========== 超时配置 ==========
config.setConnectionTimeout(30000);    // 连接超时：30秒
config.setIdleTimeout(600000);         // 空闲超时：10分钟
config.setMaxLifetime(1800000);        // 最大存活时间：30分钟

// ========== 连接测试 ==========
config.setConnectionTestQuery("SELECT 1");  // MySQL/H2
// config.setConnectionTestQuery("SELECT 1 FROM DUAL");  // Oracle

// ========== 性能优化 ==========
config.setAutoCommit(true);         // 自动提交（默认true）
config.setCachePrepStmts(true);     // 缓存PreparedStatement
config.setPrepStmtCacheSize(250);   // PreparedStatement缓存大小
config.setPrepStmtCacheSqlLimit(2048);  // SQL长度限制

// ========== 其他配置 ==========
config.setPoolName("MyApp-HikariCP");
config.setReadOnly(false);
```

### 1.5 连接池监控

```java
// 获取连接池状态
HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

System.out.println("总连接数: " + poolMXBean.getTotalConnections());
System.out.println("活动连接数: " + poolMXBean.getActiveConnections());
System.out.println("空闲连接数: " + poolMXBean.getIdleConnections());
System.out.println("等待线程数: " + poolMXBean.getThreadsAwaitingConnection());
```

---

## 📦 2. MyBatis集成

### 2.1 为什么集成MyBatis？

**MyBatis优势**：
- SQL灵活可控
- 学习曲线平缓
- 性能优秀
- 社区活跃

### 2.2 添加依赖

```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.13</version>
</dependency>
```

### 2.3 MyBatis核心概念

**核心组件**：
```
SqlSessionFactory（会话工厂）
    ├── 配置信息
    ├── 数据源
    └── Mapper映射
    
SqlSession（会话）
    ├── 执行SQL
    ├── 获取Mapper
    └── 事务管理
    
Mapper（映射器）
    └── 接口 + XML/注解
```

### 2.4 集成方式

#### 方式1：使用SqlSessionFactoryBean

```java
@Configuration
public class MyBatisConfig {
    
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) 
            throws Exception {
        
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 初始化
        factoryBean.afterPropertiesSet();
        
        return factoryBean.getObject();
    }
}
```

#### 方式2：编程式配置

```java
@Bean
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
    // 创建Configuration
    Configuration configuration = new Configuration();
    
    // 设置Environment
    Environment environment = new Environment(
        "development",
        new JdbcTransactionFactory(),
        dataSource
    );
    configuration.setEnvironment(environment);
    
    // 配置
    configuration.setMapUnderscoreToCamelCase(true);
    configuration.setCacheEnabled(true);
    
    // 注册Mapper
    configuration.addMapper(UserMapper.class);
    
    // 构建SqlSessionFactory
    return new SqlSessionFactoryBuilder().build(configuration);
}
```

### 2.5 定义Mapper接口

```java
public interface UserMapper {
    
    /**
     * 根据ID查找用户
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(@Param("id") int id);
    
    /**
     * 查找所有用户
     */
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    /**
     * 插入用户
     */
    @Insert("INSERT INTO users (name, age) VALUES (#{name}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    /**
     * 更新用户
     */
    @Update("UPDATE users SET name = #{name}, age = #{age} WHERE id = #{id}")
    int update(User user);
    
    /**
     * 删除用户
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(int id);
}
```

### 2.6 获取Mapper实例

#### 手动方式

```java
SqlSession session = sqlSessionFactory.openSession();
try {
    UserMapper mapper = session.getMapper(UserMapper.class);
    User user = mapper.findById(1);
    System.out.println(user);
} finally {
    session.close();
}
```

#### 集成到lite-spring（推荐）

**关键**：将Mapper注册为Spring Bean

```java
// 1. Mapper代理对象
public class MapperFactoryBean<T> implements InitializingBean {
    
    private Class<T> mapperInterface;
    private SqlSessionFactory sqlSessionFactory;
    private T mapperProxy;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 从SqlSessionFactory获取Mapper代理
        SqlSession sqlSession = sqlSessionFactory.openSession();
        this.mapperProxy = sqlSession.getMapper(mapperInterface);
    }
    
    public T getObject() {
        return this.mapperProxy;
    }
}

// 2. 注册为Bean
@Bean
public UserMapper userMapper(SqlSessionFactory sqlSessionFactory) {
    MapperFactoryBean<UserMapper> factoryBean = new MapperFactoryBean<>();
    factoryBean.setMapperInterface(UserMapper.class);
    factoryBean.setSqlSessionFactory(sqlSessionFactory);
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();
}

// 3. 使用
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;  // 自动注入！
    
    public User findById(int id) {
        return userMapper.findById(id);
    }
}
```

### 2.7 MyBatis XML映射方式

**UserMapper.xml**：
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.litespring.demo.mapper.UserMapper">
    
    <!-- ResultMap定义 -->
    <resultMap id="UserResultMap" type="com.litespring.demo.model.User">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="age" column="age"/>
        <result property="email" column="email"/>
    </resultMap>
    
    <!-- 查询 -->
    <select id="findById" resultMap="UserResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>
    
    <select id="findAll" resultMap="UserResultMap">
        SELECT * FROM users ORDER BY id
    </select>
    
    <select id="findByName" resultMap="UserResultMap">
        SELECT * FROM users WHERE name LIKE #{name}
    </select>
    
    <!-- 插入 -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (name, age, email)
        VALUES (#{name}, #{age}, #{email})
    </insert>
    
    <!-- 更新 -->
    <update id="update">
        UPDATE users
        SET name = #{name}, age = #{age}, email = #{email}
        WHERE id = #{id}
    </update>
    
    <!-- 删除 -->
    <delete id="delete">
        DELETE FROM users WHERE id = #{id}
    </delete>
</mapper>
```

**Mapper接口**：
```java
public interface UserMapper {
    User findById(int id);
    List<User> findAll();
    List<User> findByName(String name);
    int insert(User user);
    int update(User user);
    int delete(int id);
}
```

---

## 📋 完整集成示例

### 完整的数据访问层配置

```java
@Configuration
@ComponentScan("com.litespring.demo")
public class DataAccessConfig {
    
    /**
     * 1. 配置HikariCP数据源
     */
    @Bean
    public DataSource dataSource() {
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl("jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC");
        config.setUsername("root");
        config.setPassword("password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 连接池配置
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setConnectionTimeout(30000);
        
        return HikariDataSourceFactory.createDataSource(config);
    }
    
    /**
     * 2. 配置JdbcTemplate
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * 3. 配置MyBatis SqlSessionFactory
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) 
            throws Exception {
        
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 可以设置MyBatis配置文件位置
        // factoryBean.setConfigLocation("mybatis-config.xml");
        
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
    
    /**
     * 4. 配置Mapper扫描
     */
    @Bean
    public MapperScannerConfigurer mapperScanner(SqlSessionFactory sqlSessionFactory) {
        MapperScannerConfigurer scanner = new MapperScannerConfigurer(beanFactory);
        scanner.setBasePackage("com.litespring.demo.mapper");
        scanner.setSqlSessionFactory(sqlSessionFactory);
        return scanner;
    }
}
```

### 使用示例

```java
@Service
public class UserService {
    
    // 方式1：使用JdbcTemplate
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public User findByIdUsingJdbc(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
    }
    
    // 方式2：使用MyBatis Mapper
    @Autowired
    private UserMapper userMapper;
    
    public User findByIdUsingMyBatis(int id) {
        return userMapper.findById(id);
    }
}
```

---

## 🔧 JdbcTemplate vs MyBatis对比

| 特性 | JdbcTemplate | MyBatis |
|------|-------------|---------|
| **SQL控制** | 完全手写 | 完全手写 |
| **映射方式** | RowMapper手动映射 | 自动映射或ResultMap |
| **复杂查询** | 需要手写 | 动态SQL支持强 |
| **学习成本** | 低 | 中 |
| **灵活性** | 高 | 高 |
| **适用场景** | 简单CRUD | 复杂查询 |

### 何时使用JdbcTemplate？

**适合场景**：
- 简单的CRUD操作
- SQL语句简单
- 需要完全控制SQL
- 不想引入ORM框架

**示例**：
```java
// 简单查询
int count = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM users", 
    Integer.class
);

// 简单更新
jdbcTemplate.update(
    "UPDATE users SET status = ? WHERE id = ?", 
    "active", 123
);
```

### 何时使用MyBatis？

**适合场景**：
- 复杂的查询（多表关联、动态SQL）
- 需要结果映射
- 团队熟悉MyBatis
- 中大型项目

**示例**：
```xml
<!-- 动态SQL -->
<select id="findUsers" resultType="User">
    SELECT * FROM users
    <where>
        <if test="name != null">
            AND name LIKE #{name}
        </if>
        <if test="minAge != null">
            AND age >= #{minAge}
        </if>
    </where>
</select>
```

---

## 🎯 集成第三方库的通用模式

### 模式1：FactoryBean模式

**什么是FactoryBean？**
- 特殊的Bean，用于创建其他对象
- getObject()返回真正要使用的对象
- 适合集成第三方框架

**示例**：
```java
public class SqlSessionFactoryBean implements InitializingBean {
    
    private DataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 在初始化时创建SqlSessionFactory
        this.sqlSessionFactory = buildSqlSessionFactory();
    }
    
    public SqlSessionFactory getObject() {
        return this.sqlSessionFactory;
    }
}

// 使用
@Bean
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource);
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();  // 返回SqlSessionFactory
}
```

### 模式2：工厂方法模式

```java
public class HikariDataSourceFactory {
    
    public static DataSource createDataSource(String url, String username, String password) {
        // 创建第三方对象
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        
        return new HikariDataSource(config);
    }
}

// 使用
@Bean
public DataSource dataSource() {
    return HikariDataSourceFactory.createDataSource(...);
}
```

### 模式3：@Import导入配置

```java
// 第三方提供的配置类
public class MyBatisAutoConfiguration {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
        // ...
    }
}

// 导入配置
@Configuration
@Import(MyBatisAutoConfiguration.class)
public class AppConfig {
}
```

---

## 💡 实际项目配置示例

### 完整的配置类

```java
@Configuration
@ComponentScan("com.litespring.demo")
public class AppConfig {
    
    // ========== 数据源配置 ==========
    
    @Bean
    public DataSource dataSource() {
        DataSourceConfig config = new DataSourceConfig();
        
        // 数据库连接信息（实际项目应该从配置文件读取）
        config.setUrl("jdbc:mysql://localhost:3306/litespring_demo");
        config.setUsername("root");
        config.setPassword("your_password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // HikariCP连接池配置
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setConnectionTimeout(30000);
        config.setPoolName("LiteSpring-Demo-Pool");
        
        return HikariDataSourceFactory.createDataSource(config);
    }
    
    // ========== JdbcTemplate配置 ==========
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    // ========== MyBatis配置 ==========
    
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) 
            throws Exception {
        
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 初始化
        factoryBean.afterPropertiesSet();
        
        return factoryBean.getObject();
    }
    
    // ========== Mapper注册 ==========
    
    @Bean
    public UserMapper userMapper(SqlSessionFactory sqlSessionFactory) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        return sqlSession.getMapper(UserMapper.class);
    }
}
```

### 在Dao层使用

```java
@Repository
public class UserDaoImpl implements UserDao {
    
    // 方式1：使用JdbcTemplate
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public User findByIdWithJdbc(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            return user;
        }, id);
    }
    
    public void saveWithJdbc(User user) {
        String sql = "INSERT INTO users (name, age) VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getName(), user.getAge());
    }
}

@Repository  
public class UserMapperDao {
    
    // 方式2：使用MyBatis Mapper
    @Autowired
    private UserMapper userMapper;
    
    public User findByIdWithMyBatis(int id) {
        return userMapper.findById(id);
    }
    
    public void saveWithMyBatis(User user) {
        userMapper.insert(user);
    }
}
```

---

## 🎓 集成原理解析

### HikariCP集成原理

**关键点**：HikariDataSource实现了javax.sql.DataSource接口

```java
// HikariCP提供的类
public class HikariDataSource implements DataSource {
    @Override
    public Connection getConnection() throws SQLException {
        // 从连接池获取连接
        return pool.getConnection();
    }
}

// lite-spring使用
DataSource dataSource = new HikariDataSource(config);  // 第三方对象
JdbcTemplate template = new JdbcTemplate(dataSource);  // lite-spring对象

// 完全兼容！因为都实现了标准的DataSource接口
```

**接口是关键**：
- javax.sql.DataSource是Java标准接口
- HikariCP实现这个接口
- JdbcTemplate依赖这个接口
- **面向接口编程，完美解耦！**

### MyBatis集成原理

**关键点**：利用InitializingBean在Bean初始化时创建MyBatis对象

```java
public class SqlSessionFactoryBean implements InitializingBean {
    
    private DataSource dataSource;  // 从lite-spring容器注入
    private SqlSessionFactory sqlSessionFactory;  // MyBatis对象
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 在Bean初始化后，创建MyBatis的SqlSessionFactory
        Configuration config = new Configuration();
        config.setEnvironment(new Environment("dev", txFactory, dataSource));
        
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);
    }
    
    public SqlSessionFactory getObject() {
        return this.sqlSessionFactory;
    }
}
```

**集成流程**：
```
1. lite-spring创建SqlSessionFactoryBean
2. 注入DataSource（来自lite-spring容器）
3. 调用afterPropertiesSet（InitializingBean回调）
4. 创建MyBatis的SqlSessionFactory
5. getObject()返回SqlSessionFactory
6. SqlSessionFactory被注册到lite-spring容器
7. 其他Bean可以@Autowired注入使用
```

---

## 🎯 为什么这样设计？

### 设计原则

**1. 面向接口编程**
- DataSource是标准接口
- 任何实现都可以替换
- HikariCP、Druid、C3P0都兼容

**2. 依赖注入**
- SqlSessionFactoryBean需要DataSource
- 通过lite-spring的DI机制注入
- 自动管理依赖关系

**3. 生命周期回调**
- 利用InitializingBean接口
- 在合适的时机初始化第三方对象
- 集成到Bean生命周期

**4. FactoryBean模式**
- Bean本身是工厂
- getObject()返回真正的对象
- 适合复杂对象创建

---

## 🎊 完整示例项目

### 项目结构

```
com.litespring.demo
├── config/
│   └── DataAccessConfig.java       # 数据访问配置
├── model/
│   └── User.java                   # 实体类
├── mapper/
│   ├── UserMapper.java             # MyBatis Mapper接口
│   └── UserMapper.xml              # MyBatis XML映射
├── dao/
│   └── UserDaoImpl.java            # 使用JdbcTemplate的Dao
├── service/
│   └── UserService.java            # 服务层
└── DemoApplication.java            # 启动类
```

### 完整的使用流程

```java
// 1. 启动应用
public class DemoApplication {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(DataAccessConfig.class);
        
        UserService service = ctx.getBean(UserService.class);
        
        // 使用JdbcTemplate
        User user1 = service.findByIdWithJdbc(1);
        System.out.println("JdbcTemplate: " + user1);
        
        // 使用MyBatis
        User user2 = service.findByIdWithMyBatis(1);
        System.out.println("MyBatis: " + user2);
        
        ctx.close();
    }
}
```

---

## 📚 学习要点

### 1. 理解接口的重要性

**标准接口**：
- javax.sql.DataSource
- javax.sql.Connection
- java.sql.PreparedStatement
- java.sql.ResultSet

**为什么重要**：
- 各种实现都兼容
- 可以随意替换
- 不依赖具体实现

### 2. 理解生命周期集成

**InitializingBean的应用**：
```java
public class ThirdPartyBean implements InitializingBean {
    
    @Autowired
    private DataSource dataSource;  // lite-spring注入
    
    private ThirdPartyObject thirdPartyObject;  // 第三方对象
    
    @Override
    public void afterPropertiesSet() {
        // 使用lite-spring的Bean创建第三方对象
        this.thirdPartyObject = new ThirdPartyFactory()
            .create(dataSource);
    }
}
```

### 3. 理解依赖管理

**Maven依赖**：
```xml
<!-- 核心功能 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- 可选功能 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <optional>true</optional>
</dependency>
```

**optional的作用**：
- 不强制依赖
- 用户可以选择是否使用MyBatis
- 如果不用MyBatis，不会下载这个jar

---

理解了吗？现在让我为你创建完整的测试用例和使用文档！继续创建...🚀
