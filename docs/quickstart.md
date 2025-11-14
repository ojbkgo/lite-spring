# 快速开始指南

本指南将帮助你快速上手Lite Spring项目，开始学习和开发。

## 前置准备

### 必需环境

1. **JDK 11+**
   ```bash
   # 检查Java版本
   java -version
   # 应该显示：java version "11.x.x" 或更高
   ```

2. **Maven 3.6+**
   ```bash
   # 检查Maven版本
   mvn -version
   # 应该显示：Apache Maven 3.6.x 或更高
   ```

3. **IDE**
   - 推荐：IntelliJ IDEA（Community或Ultimate版本）
   - 备选：Eclipse、VS Code

### 推荐工具

- **Git**：版本控制
- **Postman**：API测试（第六阶段MVC会用到）
- **Maven Helper插件**：IDEA插件，方便查看依赖关系

## 项目导入

### 使用IntelliJ IDEA

1. 打开IDEA
2. 选择 `File` -> `Open`
3. 选择项目根目录下的 `pom.xml` 文件
4. 选择 "Open as Project"
5. 等待Maven自动导入依赖

### 使用Eclipse

1. 打开Eclipse
2. 选择 `File` -> `Import`
3. 选择 `Maven` -> `Existing Maven Projects`
4. 选择项目根目录
5. 点击 `Finish`

### 使用命令行

```bash
cd lite-spring
mvn clean install
```

## 项目结构说明

```
lite-spring/
├── pom.xml                   # 父POM，管理子模块和依赖版本
├── lite-spring/              # 核心框架模块
│   ├── pom.xml              # 框架模块POM
│   └── src/
│       ├── main/java/       # 框架源代码
│       └── test/java/       # 框架测试代码
├── lite-spring-demo/         # 示例项目
│   ├── pom.xml              # Demo模块POM
│   └── src/
│       ├── main/java/       # Demo源代码
│       └── main/resources/  # 配置文件
└── docs/                     # 文档目录
    ├── roadmap.md           # 学习路线图 ⭐ 必读
    ├── progress.md          # 进度记录
    ├── architecture.md      # 架构设计
    └── quickstart.md        # 本文件
```

## 第一步：阅读文档

在开始编码之前，强烈建议按以下顺序阅读文档：

1. **README.md** - 了解项目整体情况
2. **docs/roadmap.md** - 了解完整的学习路线（⭐最重要）
3. **docs/architecture.md** - 了解架构设计思路

## 第二步：运行测试

确保项目环境配置正确：

```bash
# 在项目根目录执行
mvn clean test
```

如果看到 "BUILD SUCCESS"，说明环境配置正确。

## 第三步：开始第一阶段开发

### 阶段目标

第一阶段的目标是实现一个简单的IoC容器，支持：
- Bean的定义和注册
- Bean的创建和获取
- 基于XML的配置

### 实现步骤

#### 1. 实现BeanFactory（已有接口定义）

查看 `lite-spring/src/main/java/com/litespring/core/BeanFactory.java`

#### 2. 实现SimpleBeanFactory

创建 `lite-spring/src/main/java/com/litespring/core/SimpleBeanFactory.java`

```java
public class SimpleBeanFactory implements BeanFactory {
    // 存储Bean定义
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    
    // 存储单例Bean实例
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    // TODO: 实现getBean方法
    // TODO: 实现Bean的创建逻辑
}
```

#### 3. 实现XML解析

创建 `lite-spring/src/main/java/com/litespring/core/io/XmlBeanDefinitionReader.java`

```java
public class XmlBeanDefinitionReader {
    // TODO: 解析XML配置文件
    // TODO: 构建BeanDefinition对象
}
```

#### 4. 编写测试用例

在 `lite-spring/src/test/java/` 下创建测试类：

```java
@Test
public void testGetBean() {
    BeanFactory factory = new SimpleBeanFactory();
    // 加载配置
    // 获取Bean
    // 断言
}
```

### 验证成果

完成第一阶段后，应该能够：

```java
// 创建Bean工厂
BeanFactory factory = new XmlBeanFactory(
    new ClassPathResource("beans.xml")
);

// 获取Bean
HelloService service = (HelloService) factory.getBean("helloService");

// 调用方法
String result = service.greet("World");
System.out.println(result); // 输出：Hello, World! Welcome to Lite Spring Framework.
```

## 第四步：运行Demo

第一阶段完成后，更新 `DemoApplication.java`：

```java
public class DemoApplication {
    public static void main(String[] args) {
        // 加载XML配置
        BeanFactory factory = new XmlBeanFactory(
            new ClassPathResource("beans.xml")
        );
        
        // 获取Bean
        HelloService helloService = (HelloService) factory.getBean("helloService");
        
        // 使用Bean
        System.out.println(helloService.greet("Lite Spring"));
    }
}
```

运行：

```bash
cd lite-spring-demo
mvn compile exec:java -Dexec.mainClass="com.litespring.demo.DemoApplication"
```

## 开发建议

### 1. 使用TDD（测试驱动开发）

```
编写测试 -> 运行测试（失败）-> 实现功能 -> 运行测试（通过）-> 重构代码
```

### 2. 参考Spring源码

实现某个功能后，可以对比Spring的实现：

```bash
# 克隆Spring源码（可选）
git clone https://github.com/spring-projects/spring-framework.git
```

### 3. 调试技巧

- 使用断点调试，观察Bean的创建过程
- 打印日志，理解代码执行流程
- 使用IDE的"Show Diagram"功能查看类图

### 4. 记录进度

在 `docs/progress.md` 中记录：
- 每天完成的内容
- 遇到的问题和解决方案
- 学到的新知识

### 5. 代码规范

- 遵循Java命名规范
- 添加必要的注释
- 保持代码整洁（使用IDE的格式化功能）

## 常见问题

### Q1: Maven依赖下载失败

**A**: 配置国内镜像源，编辑 `~/.m2/settings.xml`：

```xml
<mirrors>
    <mirror>
        <id>aliyun</id>
        <mirrorOf>central</mirrorOf>
        <name>Aliyun Maven</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

### Q2: 测试运行失败

**A**: 检查：
1. JDK版本是否正确（需要11+）
2. Maven依赖是否下载完整
3. 测试代码是否有语法错误

### Q3: 不知道从哪里开始

**A**: 严格按照 `docs/roadmap.md` 中的步骤进行，不要跳跃：
1. 第一阶段：简单的Bean创建
2. 第二阶段：依赖注入
3. 依次类推...

### Q4: 实现困难，看不懂Spring源码

**A**: 
1. 先自己思考如何实现
2. 实现一个简化版本
3. 再去看Spring源码，对比差异
4. Spring源码非常复杂，不要期望一次看懂

### Q5: 需要学习哪些前置知识

**A**: 
- Java基础：反射、注解、泛型
- 设计模式：工厂、单例、代理、模板方法
- XML解析：DOM或SAX
- Servlet（第六阶段需要）
- JDBC（第七阶段需要）

## 学习资源

### 推荐视频
- B站搜索"Spring源码解析"
- B站搜索"手写Spring框架"

### 推荐博客
- 江南一点雨的Spring系列
- 纯洁的微笑的Spring Boot系列

### 推荐书籍
- 《Spring源码深度解析》
- 《Spring揭秘》

## 下一步

完成项目初始化和第一阶段后，继续：

1. **第二阶段**：实现依赖注入，解决循环依赖问题
2. **第三阶段**：完善Bean生命周期管理
3. **第四阶段**：支持注解驱动开发

详细内容参见 [roadmap.md](roadmap.md)

## 获取帮助

- 查看文档：`docs/` 目录下的所有文档
- 参考测试：`lite-spring/src/test/` 目录下的测试用例
- 查看示例：`lite-spring-demo/` 目录

---

祝学习顺利！Remember: The best way to learn a framework is to build one! 💪

