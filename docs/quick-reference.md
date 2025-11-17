# Lite Spring 快速参考手册

本文档提供lite-spring框架的快速使用参考，涵盖所有已实现的功能。

---

## 📚 目录

1. [IoC容器基础](#1-ioc容器基础)
2. [依赖注入](#2-依赖注入)
3. [Bean生命周期](#3-bean生命周期)
4. [注解驱动](#4-注解驱动)
5. [AOP](#5-aop)

---

## 1. IoC容器基础

### XML配置方式

**beans.xml**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean id="helloService" 
          class="com.example.HelloServiceImpl"
          scope="singleton">
    </bean>
</beans>
```

**Java代码**：
```java
// 创建容器
BeanFactory factory = new XmlBeanFactory(
    new ClassPathResource("beans.xml")
);

// 获取Bean
HelloService service = factory.getBean("helloService", HelloService.class);
```

**支持的scope**：
- `singleton`（默认）：单例
- `prototype`：原型，每次创建新实例

---

## 2. 依赖注入

### Setter注入

**XML配置**：
```xml
<bean id="userDao" class="com.example.UserDaoImpl"/>

<bean id="userService" class="com.example.UserServiceImpl">
    <!-- Bean引用 -->
    <property name="userDao" ref="userDao"/>
    <!-- 简单值 -->
    <property name="maxRetry" value="3"/>
</bean>
```

**Java代码**：
```java
public class UserServiceImpl {
    private UserDao userDao;
    private int maxRetry;
    
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
    
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
}
```

### 构造器注入

**XML配置**：
```xml
<bean id="orderService" class="com.example.OrderServiceImpl">
    <constructor-arg ref="orderDao"/>
    <constructor-arg value="100"/>
</bean>
```

**Java代码**：
```java
public class OrderServiceImpl {
    private final OrderDao orderDao;
    private final int maxSize;
    
    public OrderServiceImpl(OrderDao orderDao, int maxSize) {
        this.orderDao = orderDao;
        this.maxSize = maxSize;
    }
}
```

### 支持的类型转换

- String
- int/Integer、long/Long、double/Double等
- boolean/Boolean（支持true/false、yes/no、1/0）

---

## 3. Bean生命周期

### 初始化方法

**方式1：实现接口**
```java
public class MyBean implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("初始化");
    }
}
```

**方式2：XML配置**
```xml
<bean id="myBean" class="com.example.MyBean" init-method="init"/>
```

```java
public class MyBean {
    public void init() {
        System.out.println("初始化");
    }
}
```

### 销毁方法

**方式1：实现接口**
```java
public class MyBean implements DisposableBean {
    @Override
    public void destroy() throws Exception {
        System.out.println("销毁");
    }
}
```

**方式2：XML配置**
```xml
<bean id="myBean" class="com.example.MyBean" destroy-method="cleanup"/>
```

### Aware接口

```java
public class MyBean implements BeanNameAware, BeanFactoryAware {
    
    @Override
    public void setBeanName(String name) {
        System.out.println("我的Bean名称: " + name);
    }
    
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        // 可以从容器获取其他Bean
    }
}
```

### BeanPostProcessor

```java
public class MyBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 初始化前处理
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 初始化后处理（可以返回代理对象）
        return bean;
    }
}
```

**完整生命周期顺序**：
```
1. 构造函数
2. 属性注入
3. BeanNameAware.setBeanName()
4. BeanFactoryAware.setBeanFactory()
5. BeanPostProcessor.postProcessBeforeInitialization()
6. InitializingBean.afterPropertiesSet()
7. init-method
8. BeanPostProcessor.postProcessAfterInitialization()
9. Bean就绪
10. DisposableBean.destroy()
11. destroy-method
```

---

## 4. 注解驱动

### 定义组件

```java
// Service层
@Service
public class UserService {
}

// Dao层
@Repository
public class UserDaoImpl implements UserDao {
}

// Controller层
@Controller
public class UserController {
}

// 通用组件
@Component
public class MyComponent {
}
```

### 自动装配

```java
@Service
public class UserService {
    
    // 按类型自动装配
    @Autowired
    private UserDao userDao;
    
    // 如果有多个实现，用@Qualifier指定
    @Autowired
    @Qualifier("userDaoImpl")
    private UserDao specificDao;
    
    // 注入配置值
    @Value("3")
    private int maxRetry;
}
```

### 配置类

```java
@Configuration
@ComponentScan("com.example")  // 扫描包
public class AppConfig {
    
    // 可以在这里定义Bean（后续支持）
    // @Bean
    // public DataSource dataSource() {
    //     return new DataSource();
    // }
}
```

### 创建容器

```java
// 方式1：通过配置类
AnnotationConfigApplicationContext ctx = 
    new AnnotationConfigApplicationContext(AppConfig.class);

// 方式2：直接扫描包
AnnotationConfigApplicationContext ctx = 
    new AnnotationConfigApplicationContext("com.example");

// 获取Bean
UserService service = ctx.getBean(UserService.class);

// 关闭容器
ctx.close();
```

---

## 5. AOP

### 定义通知

**前置通知**：
```java
public class LoggingAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) {
        System.out.println("执行方法: " + method.getName());
    }
}
```

**返回后通知**：
```java
public class AfterAdvice implements AfterReturningAdvice {
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) {
        System.out.println("方法返回: " + returnValue);
    }
}
```

**环绕通知（最强大）**：
```java
public class PerformanceAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        
        // 执行目标方法
        Object result = invocation.proceed();
        
        long end = System.currentTimeMillis();
        System.out.println("耗时: " + (end - start) + "ms");
        
        return result;
    }
}
```

### 定义切点

```java
// 方法名匹配
NameMatchPointcut pointcut = new NameMatchPointcut();
pointcut.addMethodName("saveUser");
pointcut.addMethodName("deleteUser");
```

### 组合Advisor

```java
Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
```

### 创建代理

**手动方式**：
```java
UserService target = new UserServiceImpl();

ProxyFactory proxyFactory = new ProxyFactory();
proxyFactory.setTarget(target);
proxyFactory.addAdvisor(advisor);

UserService proxy = (UserService) proxyFactory.getProxy();
proxy.saveUser("Tom");  // 自动触发AOP逻辑
```

**自动方式**（集成到容器）：
```java
// 1. 注册目标Bean
factory.registerBeanDefinition("userService", ...);

// 2. 注册Advisor
factory.registerBeanDefinition("loggingAdvisor", ...);

// 3. 注册自动代理创建器
DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
creator.setBeanFactory(factory);
factory.addBeanPostProcessor(creator);

// 4. 获取Bean（自动返回代理对象）
UserService service = factory.getBean("userService", UserService.class);
service.saveUser("Tom");  // 自动应用AOP
```

---

## 🎯 常用API

### BeanFactory

```java
// 按名称获取
Object getBean(String name);
<T> T getBean(String name, Class<T> requiredType);

// 按类型获取（v4新增）
<T> T getBean(Class<T> requiredType);

// 按类型获取所有
<T> Map<String, T> getBeansOfType(Class<T> type);

// 判断是否包含
boolean containsBean(String name);
```

### AnnotationConfigApplicationContext

```java
// 创建容器
AnnotationConfigApplicationContext ctx = 
    new AnnotationConfigApplicationContext(AppConfig.class);

// 获取Bean
UserService service = ctx.getBean(UserService.class);

// 关闭容器
ctx.close();
```

### ProxyFactory

```java
// 创建代理
ProxyFactory factory = new ProxyFactory();
factory.setTarget(target);
factory.addAdvisor(advisor);
Object proxy = factory.getProxy();
```

---

## 🔧 配置示例

### 完整的XML配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <!-- Dao层 -->
    <bean id="userDao" class="com.example.UserDaoImpl"/>
    
    <!-- Service层：Setter注入 -->
    <bean id="userService" 
          class="com.example.UserServiceImpl"
          scope="singleton"
          init-method="init"
          destroy-method="destroy">
        <property name="userDao" ref="userDao"/>
        <property name="maxRetry" value="3"/>
    </bean>
    
    <!-- Service层：构造器注入 -->
    <bean id="orderService" class="com.example.OrderServiceImpl">
        <constructor-arg ref="orderDao"/>
        <constructor-arg value="100"/>
    </bean>
    
    <!-- 循环依赖示例 -->
    <bean id="a" class="com.example.A">
        <property name="b" ref="b"/>
    </bean>
    <bean id="b" class="com.example.B">
        <property name="a" ref="a"/>
    </bean>
</beans>
```

### 完整的注解配置

```java
// 1. 配置类
@Configuration
@ComponentScan("com.example")
public class AppConfig {
}

// 2. Dao层
@Repository
public class UserDaoImpl implements UserDao {
}

// 3. Service层
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
    
    @Value("3")
    private int maxRetry;
}

// 4. 使用
AnnotationConfigApplicationContext ctx = 
    new AnnotationConfigApplicationContext(AppConfig.class);
UserService service = ctx.getBean(UserService.class);
```

---

## ⚠️ 注意事项

### 循环依赖

**可以解决**：
- ✅ 单例Bean的Setter注入循环依赖

**无法解决**：
- ❌ 构造器注入的循环依赖
- ❌ 原型Bean的循环依赖

### JDK代理限制

**要求**：
- 目标类必须实现接口
- 代理的是接口方法

**如果没有接口**：
- 需要CGLIB代理（暂未实现）
- 或者让类实现接口

### 按类型装配

**要求**：
- 类型只能有一个Bean实现
- 如果有多个，使用@Qualifier指定

---

## 🎯 完整示例

### 综合示例（XML方式）

```xml
<!-- beans.xml -->
<beans>
    <bean id="userDao" class="com.example.UserDaoImpl"/>
    
    <bean id="userService" 
          class="com.example.UserServiceImpl"
          init-method="init"
          destroy-method="destroy">
        <property name="userDao" ref="userDao"/>
        <property name="serviceName" value="UserService"/>
    </bean>
</beans>
```

```java
// 使用
public class App {
    public static void main(String[] args) {
        // 创建容器
        BeanFactory factory = new XmlBeanFactory(
            new ClassPathResource("beans.xml")
        );
        
        // 获取Bean
        UserService service = factory.getBean("userService", UserService.class);
        
        // 使用Bean
        service.saveUser("Tom");
        
        // 如果factory是DefaultBeanFactory，可以关闭
        if (factory instanceof DefaultBeanFactory_v3) {
            ((DefaultBeanFactory_v3) factory).close();
        }
    }
}
```

### 综合示例（注解方式）

```java
// 1. Dao层
@Repository
public class UserDaoImpl implements UserDao {
    @Override
    public void save(String username) {
        System.out.println("保存用户: " + username);
    }
}

// 2. Service层
@Service
public class UserService implements InitializingBean {
    
    @Autowired
    private UserDao userDao;
    
    @Value("3")
    private int maxRetry;
    
    @Override
    public void afterPropertiesSet() {
        System.out.println("UserService初始化完成");
    }
    
    public void saveUser(String username) {
        System.out.println("最大重试: " + maxRetry);
        userDao.save(username);
    }
}

// 3. 配置类
@Configuration
@ComponentScan("com.example")
public class AppConfig {
}

// 4. 启动类
public class App {
    public static void main(String[] args) {
        // 创建容器
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        // 获取Bean
        UserService service = ctx.getBean(UserService.class);
        
        // 使用
        service.saveUser("Tom");
        
        // 关闭
        ctx.close();
    }
}
```

### 综合示例（AOP）

```java
// 1. 定义目标接口和实现
public interface UserService {
    void saveUser(String username);
    String findUser(int id);
}

public class UserServiceImpl implements UserService {
    @Override
    public void saveUser(String username) {
        System.out.println("保存用户: " + username);
    }
    
    @Override
    public String findUser(int id) {
        return "User-" + id;
    }
}

// 2. 定义通知
public class LoggingAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) {
        System.out.println("【日志】执行方法: " + method.getName());
        System.out.println("【日志】参数: " + Arrays.toString(args));
    }
}

public class PerformanceAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long end = System.currentTimeMillis();
        
        System.out.println("【性能】耗时: " + (end - start) + "ms");
        return result;
    }
}

// 3. 组装和使用
public class AopDemo {
    public static void main(String[] args) {
        // 创建目标对象
        UserService target = new UserServiceImpl();
        
        // 创建切点
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("saveUser");
        pointcut.addMethodName("findUser");
        
        // 创建代理工厂
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        
        // 添加Advisor
        proxyFactory.addAdvisor(
            new DefaultPointcutAdvisor(pointcut, new LoggingAdvice())
        );
        proxyFactory.addAdvisor(
            new DefaultPointcutAdvisor(pointcut, new PerformanceAdvice())
        );
        
        // 获取代理
        UserService proxy = (UserService) proxyFactory.getProxy();
        
        // 调用方法（自动触发AOP）
        System.out.println("\n========== 调用saveUser ==========");
        proxy.saveUser("Tom");
        
        System.out.println("\n========== 调用findUser ==========");
        String user = proxy.findUser(100);
        System.out.println("返回: " + user);
    }
}
```

**输出**：
```
========== 调用saveUser ==========
【性能】方法开始
【日志】执行方法: saveUser
【日志】参数: [Tom]
保存用户: Tom
【性能】耗时: 2ms

========== 调用findUser ==========
【性能】方法开始
【日志】执行方法: findUser
【日志】参数: [100]
查找用户: 100
【性能】耗时: 1ms
返回: User-100
```

---

## 🚀 运行测试

```bash
cd /Users/ziyuewen/Devspace/myprj/lite-spring

# 运行所有测试
mvn test

# 运行指定阶段测试
mvn test -Dtest="com.litespring.test.v1.*"  # 第一阶段
mvn test -Dtest="com.litespring.test.v2.*"  # 第二阶段
mvn test -Dtest="com.litespring.test.v3.*"  # 第三阶段
mvn test -Dtest="com.litespring.test.v4.*"  # 第四阶段
mvn test -Dtest="com.litespring.test.v5.*"  # 第五阶段

# 运行Demo
cd lite-spring-demo
mvn compile exec:java -Dexec.mainClass="com.litespring.demo.DemoApplication"
```

---

## 📚 更多文档

- **项目总结**：[project-summary.md](./project-summary.md)
- **循环依赖详解**：[circular-dependency-explained.md](./circular-dependency-explained.md)
- **实现指南索引**：[implementation-guide.md](./implementation-guide.md)

---

**祝使用愉快！** 🎉

