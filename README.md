# Lite Spring Framework

一个从零开始实现的轻量级Spring框架，用于深入学习和理解Spring、Spring Boot、Spring MVC的核心原理。

## 📚 项目介绍

Lite Spring是一个教育性质的项目，通过逐步实现Spring框架的核心功能，帮助开发者深入理解：

- **IoC (控制反转)** 和 **DI (依赖注入)** 的实现原理
- **AOP (面向切面编程)** 的底层机制
- **Spring MVC** 的请求处理流程
- **事务管理** 的实现方式
- **Spring Boot** 的自动配置原理

## 🎯 学习目标

通过这个项目，你将学习到：

1. 如何设计和实现一个IoC容器
2. Bean的生命周期管理
3. 循环依赖问题的解决方案（三级缓存）
4. JDK动态代理和CGLIB代理的应用
5. DispatcherServlet的工作原理
6. 声明式事务的实现机制
7. 注解处理和反射的高级应用
8. 框架设计的常用设计模式

## 🏗️ 项目结构

```
lite-spring/
├── lite-spring/              # 核心框架模块
│   └── src/
│       ├── main/java/
│       │   └── com/litespring/
│       │       ├── core/     # IoC核心
│       │       ├── aop/      # AOP模块
│       │       ├── web/      # Web MVC模块
│       │       ├── jdbc/     # 数据访问模块
│       │       ├── tx/       # 事务管理模块
│       │       └── util/     # 工具类
│       └── test/java/        # 单元测试
│
├── lite-spring-demo/         # 示例Web项目
│   └── src/
│       ├── main/java/
│       │   └── com/litespring/demo/
│       │       ├── controller/
│       │       ├── service/
│       │       └── DemoApplication.java
│       └── main/resources/
│           ├── beans.xml
│           └── application.properties
│
└── docs/                     # 文档
    ├── roadmap.md           # 学习路线图（必读）
    ├── progress.md          # 开发进度记录
    └── architecture.md      # 架构设计文档
```

## 🚀 快速开始

### 环境要求

- JDK 11 或更高版本
- Maven 3.6+
- IDE（推荐IntelliJ IDEA）

### 克隆项目

```bash
cd lite-spring
```

### 编译项目

```bash
mvn clean install
```

### 运行测试

```bash
mvn test
```

### 运行Demo

```bash
cd lite-spring-demo
mvn compile exec:java -Dexec.mainClass="com.litespring.demo.DemoApplication"
```

## 📖 学习路线

详细的学习路线请查看 [docs/roadmap.md](docs/roadmap.md)

### 开发阶段

- [x] **第零阶段**：项目初始化
- [ ] **第一阶段**：IoC容器基础 - 实现Bean工厂和XML配置解析
- [ ] **第二阶段**：依赖注入 - 支持构造器注入和setter注入
- [ ] **第三阶段**：Bean生命周期 - 实现初始化和销毁回调
- [ ] **第四阶段**：注解驱动 - 支持@Component、@Autowired等注解
- [ ] **第五阶段**：AOP - 实现切面编程
- [ ] **第六阶段**：MVC框架 - 实现DispatcherServlet和请求映射
- [ ] **第七阶段**：数据访问 - 实现JdbcTemplate
- [ ] **第八阶段**：事务管理 - 支持声明式事务
- [ ] **第九阶段**：自动配置 - 实现类似Spring Boot的启动机制
- [ ] **第十阶段**：完善优化 - 性能优化和功能完善

## 🔧 当前实现状态

### ✅ 已完成（核心功能）

**第一阶段：IoC容器基础**
- BeanFactory、BeanDefinition
- XML配置解析
- Bean的创建和获取
- 单例Bean缓存

**第二阶段：依赖注入**
- Setter注入和构造器注入
- Bean引用和简单值注入
- **三级缓存解决循环依赖** ⭐
- 类型转换

**第三阶段：Bean生命周期**
- InitializingBean和DisposableBean接口
- init-method和destroy-method
- **BeanPostProcessor扩展点** ⭐
- Aware接口（BeanNameAware、BeanFactoryAware）

**第四阶段：注解驱动**
- @Component、@Service、@Repository、@Controller
- @Autowired自动装配
- @Value值注入
- @Configuration和@ComponentScan
- AnnotationConfigApplicationContext

**第五阶段：AOP**
- JDK动态代理
- Pointcut切点
- Before/After/Around通知
- 拦截器链
- 自动代理创建

### 📋 可选阶段

- 第六阶段：MVC框架
- 第七阶段：数据访问
- 第八阶段：事务管理
- 第九阶段：自动配置

## 📚 核心概念

### IoC容器

控制反转（Inversion of Control）是Spring框架的核心。传统程序中，对象的创建和依赖关系由程序代码控制；而在IoC容器中，对象的创建和依赖关系由容器控制。

```java
// 传统方式
UserService userService = new UserServiceImpl();
userService.setUserDao(new UserDaoImpl());

// IoC方式
UserService userService = (UserService) beanFactory.getBean("userService");
```

### 依赖注入

依赖注入（Dependency Injection）是实现IoC的一种方式，通过容器动态地将依赖对象注入到组件中。

### Bean生命周期

Bean从创建到销毁的完整过程：
1. 实例化
2. 属性注入
3. 初始化前处理
4. 初始化
5. 初始化后处理
6. 使用
7. 销毁

## 🛠️ 技术栈

- **核心语言**：Java 11
- **构建工具**：Maven
- **测试框架**：JUnit 5
- **Web容器**：Servlet 4.0 / Embedded Tomcat
- **日志**：（待添加）

## 📝 开发建议

1. **按阶段学习**：不要跳跃式开发，每个阶段都有其独特的学习价值
2. **编写测试**：使用TDD（测试驱动开发）方式，先写测试再实现功能
3. **对比Spring源码**：实现后对比Spring的实现，学习优秀设计
4. **记录笔记**：在 `docs/progress.md` 中记录学习过程和心得
5. **重构代码**：随着理解加深，不断优化代码结构

## 🤝 贡献

这是一个个人学习项目，但欢迎：
- 提出改进建议
- 报告Bug
- 分享学习心得
- 完善文档

## 📖 参考资料

### 书籍
- 《Spring源码深度解析》- 郝佳
- 《Spring揭秘》- 王福强
- 《精通Spring 4.x 企业应用开发实战》

### 在线资源
- [Spring Framework官方文档](https://spring.io/projects/spring-framework)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Spring源码GitHub仓库](https://github.com/spring-projects/spring-framework)

### 推荐阅读
- 《设计模式：可复用面向对象软件的基础》
- 《Effective Java》
- 《Java编程思想》

## ⚠️ 免责声明

本项目仅用于学习和教育目的，不建议在生产环境中使用。如需在生产环境中使用IoC容器，请使用官方的Spring Framework。

## 📄 License

MIT License - 详见 LICENSE 文件

## 💬 联系方式

如有问题或建议，欢迎提Issue讨论。

---

**记住**：学习框架的最好方式就是自己实现一个！加油！💪

