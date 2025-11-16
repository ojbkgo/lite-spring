# Bean生命周期：初始化和销毁方法详解

## 📚 基本概念

### 为什么需要初始化方法？

在某些场景下，仅仅创建Bean实例是不够的，还需要进行一些初始化操作：

**常见场景**：
1. **数据库连接池**：创建后需要建立连接
2. **文件处理器**：需要打开文件句柄
3. **网络服务**：需要启动监听端口
4. **缓存管理器**：需要预加载数据
5. **定时任务**：需要启动调度器

**示例**：数据源Bean
```java
public class DataSource {
    private String url;
    private Connection connection;
    
    // 初始化方法：建立数据库连接
    public void connect() {
        this.connection = DriverManager.getConnection(url);
        System.out.println("数据库连接已建立");
    }
    
    // 销毁方法：关闭连接
    public void close() {
        if (connection != null) {
            connection.close();
            System.out.println("数据库连接已关闭");
        }
    }
}
```

### 为什么需要销毁方法？

当容器关闭时，需要清理资源，避免：
- 内存泄漏
- 连接未关闭
- 文件句柄泄漏
- 线程未停止

---

## 🔧 使用方式

### 1. 在XML中配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <!-- 基本配置 -->
    <bean id="lifecycleService" 
          class="com.litespring.test.v1.service.LifecycleService"
          init-method="init"
          destroy-method="destroy">
    </bean>
    
    <!-- 数据源示例 -->
    <bean id="dataSource" 
          class="com.example.DataSource"
          init-method="connect"
          destroy-method="close">
        <property name="url" value="jdbc:mysql://localhost:3306/test"/>
    </bean>
    
    <!-- 线程池示例 -->
    <bean id="threadPool" 
          class="com.example.ThreadPoolExecutor"
          init-method="start"
          destroy-method="shutdown">
    </bean>
</beans>
```

### 2. Java类中定义方法

```java
public class MyService {
    
    /**
     * 初始化方法
     * - 方法名可以自定义（但要和XML配置一致）
     * - 必须是public void 无参方法
     * - 不能抛出检查异常
     */
    public void init() {
        // 初始化逻辑
        System.out.println("初始化资源");
    }
    
    /**
     * 销毁方法
     * - 方法名可以自定义（但要和XML配置一致）
     * - 必须是public void 无参方法
     * - 不能抛出检查异常
     */
    public void destroy() {
        // 清理逻辑
        System.out.println("释放资源");
    }
}
```

---

## ⚠️ 重要说明：当前实现状态

### 第一阶段（当前）

在第一阶段，我们已经在 `BeanDefinition` 中定义了这些字段：
- `initMethodName` - 初始化方法名
- `destroyMethodName` - 销毁方法名

在 `XmlBeanDefinitionReader` 中也已经解析了这些配置。

**但是**，`DefaultBeanFactory` **还没有实现调用这些方法的逻辑**！

这意味着：
- ✅ 可以在XML中配置 init-method 和 destroy-method
- ✅ 配置会被解析并存储在BeanDefinition中
- ❌ **但不会真正调用这些方法**（第三阶段才实现）

### 第三阶段（即将实现）

在第三阶段，我们会完善Bean的生命周期管理：
1. 在 `createBean()` 方法中，创建实例后调用init方法
2. 实现容器关闭时调用destroy方法
3. 支持 `InitializingBean` 和 `DisposableBean` 接口
4. 支持 `@PostConstruct` 和 `@PreDestroy` 注解

---

## 💡 实现逻辑预览

### 如何调用初始化方法（第三阶段会实现）

```java
// 在 DefaultBeanFactory.createBean() 中
private Object createBean(BeanDefinition bd) throws BeansException {
    // 1. 创建实例
    Object bean = instantiateBean(bd);
    
    // 2. 设置属性（第二阶段实现）
    populateBean(bean, bd);
    
    // 3. 调用初始化方法（第三阶段实现）
    invokeInitMethod(bean, bd);
    
    return bean;
}

// 调用初始化方法
private void invokeInitMethod(Object bean, BeanDefinition bd) {
    String initMethodName = bd.getInitMethodName();
    if (initMethodName != null && !initMethodName.isEmpty()) {
        try {
            Method initMethod = bean.getClass().getMethod(initMethodName);
            initMethod.invoke(bean);
        } catch (Exception e) {
            throw new BeansException("初始化方法调用失败", e);
        }
    }
}
```

### 如何调用销毁方法（第三阶段会实现）

```java
// 需要实现容器关闭方法
public void close() {
    // 遍历所有单例Bean
    for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
        String beanName = entry.getKey();
        Object bean = entry.getValue();
        BeanDefinition bd = getBeanDefinition(beanName);
        
        // 调用销毁方法
        invokeDestroyMethod(bean, bd);
    }
}

// 调用销毁方法
private void invokeDestroyMethod(Object bean, BeanDefinition bd) {
    String destroyMethodName = bd.getDestroyMethodName();
    if (destroyMethodName != null && !destroyMethodName.isEmpty()) {
        try {
            Method destroyMethod = bean.getClass().getMethod(destroyMethodName);
            destroyMethod.invoke(bean);
        } catch (Exception e) {
            // 销毁方法失败不应该影响其他Bean的销毁
            System.err.println("销毁方法调用失败: " + e.getMessage());
        }
    }
}
```

---

## 🎯 实际使用场景

### 场景1：数据库连接池

```xml
<bean id="dataSource" 
      class="com.example.MyDataSource"
      init-method="initialize"
      destroy-method="close">
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="password"/>
</bean>
```

```java
public class MyDataSource {
    private String url;
    private Connection connection;
    
    public void initialize() {
        // 建立数据库连接
        this.connection = DriverManager.getConnection(url);
    }
    
    public void close() {
        // 关闭连接
        if (connection != null) {
            connection.close();
        }
    }
}
```

### 场景2：缓存预加载

```xml
<bean id="cacheManager" 
      class="com.example.CacheManager"
      init-method="preloadCache"
      destroy-method="clearCache">
</bean>
```

```java
public class CacheManager {
    private Map<String, Object> cache;
    
    public void preloadCache() {
        // 预加载常用数据
        cache = new HashMap<>();
        cache.put("config", loadConfig());
        cache.put("users", loadUsers());
    }
    
    public void clearCache() {
        // 清空缓存
        if (cache != null) {
            cache.clear();
        }
    }
}
```

### 场景3：定时任务

```xml
<bean id="scheduler" 
      class="com.example.TaskScheduler"
      init-method="start"
      destroy-method="stop">
</bean>
```

```java
public class TaskScheduler {
    private ScheduledExecutorService executor;
    
    public void start() {
        // 启动定时任务
        executor = Executors.newScheduledThreadPool(5);
        executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
    }
    
    public void stop() {
        // 停止定时任务
        if (executor != null) {
            executor.shutdown();
        }
    }
}
```

---

## 📋 方法签名要求

### 初始化方法要求

```java
// ✅ 正确的签名
public void init() { }
public void initialize() { }
public void afterPropertiesSet() { }

// ❌ 错误的签名
private void init() { }              // 不能是private
public String init() { }             // 必须返回void
public void init(String param) { }   // 不能有参数
public void init() throws Exception { }  // 不能抛检查异常
```

### 销毁方法要求

```java
// ✅ 正确的签名
public void destroy() { }
public void close() { }
public void shutdown() { }

// ❌ 错误的签名
private void destroy() { }           // 不能是private
public int destroy() { }             // 必须返回void
public void destroy(int code) { }    // 不能有参数
public void destroy() throws Exception { }  // 不能抛检查异常
```

---

## 🔄 Spring中的多种方式

在真实的Spring中，有3种方式指定初始化和销毁方法：

### 1. XML配置方式（第一阶段支持）

```xml
<bean id="service" class="..." 
      init-method="init" 
      destroy-method="destroy"/>
```

### 2. 实现接口方式（第三阶段实现）

```java
public class MyService implements InitializingBean, DisposableBean {
    
    @Override
    public void afterPropertiesSet() {
        // 初始化逻辑
    }
    
    @Override
    public void destroy() {
        // 销毁逻辑
    }
}
```

### 3. 注解方式（第四阶段实现）

```java
public class MyService {
    
    @PostConstruct
    public void init() {
        // 初始化逻辑
    }
    
    @PreDestroy
    public void destroy() {
        // 销毁逻辑
    }
}
```

---

## 🎓 学习建议

### 当前阶段（第一阶段）

1. **理解概念**：知道初始化和销毁方法的作用
2. **配置尝试**：在XML中配置这些属性
3. **查看解析**：看 `XmlBeanDefinitionReader` 如何解析
4. **等待实现**：第三阶段会实现真正的调用

### 第三阶段

到了第三阶段，你将：
1. 实现 `invokeInitMethod()` 方法
2. 实现 `invokeDestroyMethod()` 方法
3. 实现容器的 `close()` 方法
4. 支持接口方式和注解方式

---

## 💡 现在可以做的

虽然第一阶段还没实现调用逻辑，但你可以：

1. **在XML中配置**：
   ```xml
   <bean id="lifecycleService" 
         class="com.litespring.test.v1.service.LifecycleService"
         init-method="init"
         destroy-method="destroy">
   </bean>
   ```

2. **验证配置被解析**：
   ```java
   BeanDefinition bd = factory.getBeanDefinition("lifecycleService");
   System.out.println("Init方法: " + bd.getInitMethodName());    // 输出：init
   System.out.println("Destroy方法: " + bd.getDestroyMethodName()); // 输出：destroy
   ```

3. **查看日志输出**：
   运行时只会看到构造函数被调用，init和destroy方法不会被调用

---

## 🎯 总结

| 方面 | 第一阶段（现在） | 第三阶段（未来） |
|------|----------------|----------------|
| **BeanDefinition** | ✅ 已支持字段 | ✅ 保持不变 |
| **XML解析** | ✅ 已解析配置 | ✅ 保持不变 |
| **init方法调用** | ❌ 未实现 | ✅ 会实现 |
| **destroy方法调用** | ❌ 未实现 | ✅ 会实现 |
| **容器关闭** | ❌ 未实现 | ✅ 会实现 |

**记住**：这是框架的渐进式开发，每个阶段都有其重点。第一阶段重点是Bean的创建和获取，第三阶段会完善生命周期管理！

有问题随时问我！🚀

