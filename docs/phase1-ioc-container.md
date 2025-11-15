# 第一阶段：IoC容器基础实现指南

## 阶段目标

实现一个简单的IoC（控制反转）容器，支持：
- Bean的定义和注册
- Bean的创建和获取
- 基于XML的Bean配置
- 单例Bean缓存

完成后，你将能够：
```java
// 从XML加载Bean配置
BeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans.xml"));

// 获取Bean实例
HelloService service = (HelloService) factory.getBean("helloService");

// 使用Bean
service.greet("World");
```

---

## 理论基础

### 什么是IoC（控制反转）？

**传统方式**：对象的创建和依赖关系由程序代码直接控制
```java
public class UserController {
    private UserService userService;
    
    public UserController() {
        // 程序员手动创建依赖对象
        this.userService = new UserServiceImpl();
    }
}
```

**IoC方式**：对象的创建和依赖关系由容器控制
```java
public class UserController {
    private UserService userService;
    
    // 容器会自动注入依赖
    // 程序员只需要声明需要什么，不需要关心如何创建
}
```

**核心思想**：
- 不再自己new对象，而是由容器创建
- 控制权从程序代码转移到容器
- 降低耦合，提高可测试性

### IoC容器的核心职责

1. **存储Bean的定义（元数据）**
   - Bean的类名
   - Bean的作用域（单例/原型）
   - Bean的其他配置信息

2. **创建Bean实例**
   - 通过反射实例化对象
   - 处理构造函数
   - 处理异常情况

3. **管理Bean的生命周期**
   - 缓存单例Bean
   - 在需要时创建原型Bean
   - （后续阶段）初始化和销毁

4. **提供Bean的访问接口**
   - getBean(name)
   - getBean(name, type)

---

## 核心组件设计

### 1. BeanDefinition - Bean定义

**作用**：存储Bean的元数据信息

**需要包含的信息**：
- `beanClassName`: Bean的完全限定类名（如 "com.example.UserService"）
- `scope`: 作用域（"singleton" 或 "prototype"）
- `lazyInit`: 是否懒加载
- `initMethodName`: 初始化方法名（第三阶段会用）
- `destroyMethodName`: 销毁方法名（第三阶段会用）

**设计思考**：
- Q: 为什么不直接存储Class对象？
- A: 因为需要延迟加载，只有在真正需要时才加载类

**实现提示**：
```java
public class BeanDefinition {
    // 存储Bean的类名
    private String beanClassName;
    
    // 默认是单例
    private String scope = "singleton";
    
    // 提供判断方法
    public boolean isSingleton() {
        return "singleton".equals(scope);
    }
    
    // ... 其他属性和方法
}
```

---

### 2. BeanFactory - Bean工厂接口

**作用**：IoC容器的顶层接口，定义获取Bean的基本方法

**核心方法**：
```java
public interface BeanFactory {
    // 根据名称获取Bean
    Object getBean(String name);
    
    // 根据名称和类型获取Bean（类型安全）
    <T> T getBean(String name, Class<T> requiredType);
    
    // 判断是否包含指定Bean
    boolean containsBean(String name);
}
```

**设计思考**：
- Q: 为什么需要两个getBean方法？
- A: 一个返回Object需要强转，另一个使用泛型更安全

---

### 3. BeanDefinitionRegistry - Bean定义注册中心

**作用**：管理BeanDefinition的注册和获取

**核心方法**：
```java
public interface BeanDefinitionRegistry {
    // 注册Bean定义
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);
    
    // 获取Bean定义
    BeanDefinition getBeanDefinition(String beanName);
    
    // 判断是否包含Bean定义
    boolean containsBeanDefinition(String beanName);
}
```

**实现提示**：
- 使用 `Map<String, BeanDefinition>` 存储
- 考虑线程安全问题（可以先用普通HashMap，后续优化）

---

### 4. DefaultBeanFactory - 默认Bean工厂实现

**作用**：实现BeanFactory和BeanDefinitionRegistry接口，是IoC容器的核心

**需要的数据结构**：
```java
public class DefaultBeanFactory implements BeanFactory, BeanDefinitionRegistry {
    // 存储Bean定义：beanName -> BeanDefinition
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    
    // 存储单例Bean实例：beanName -> Bean实例
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    // ... 实现方法
}
```

**核心逻辑 - getBean(String name)**：
```
1. 检查Bean定义是否存在
   - 不存在 -> 抛出异常

2. 如果是单例Bean
   - 检查单例缓存中是否已有实例
   - 有 -> 直接返回
   - 无 -> 创建实例，放入缓存，返回

3. 如果是原型Bean
   - 每次都创建新实例

4. 创建Bean实例的步骤：
   - 获取BeanDefinition
   - 通过beanClassName加载Class
   - 通过反射调用无参构造函数创建实例
   - 返回实例
```

**关键点**：
- 单例Bean需要缓存，避免重复创建
- 使用 `ConcurrentHashMap` 保证线程安全
- 异常处理：类不存在、构造函数不存在等

---

### 5. Resource - 资源抽象

**作用**：统一访问不同来源的配置文件

**接口设计**：
```java
public interface Resource {
    // 获取输入流
    InputStream getInputStream() throws IOException;
    
    // 判断资源是否存在
    boolean exists();
    
    // 获取描述信息（用于调试）
    String getDescription();
}
```

**实现类**：
- `ClassPathResource`: 从classpath加载资源
- `FileSystemResource`: 从文件系统加载（可选，后续实现）

**ClassPathResource实现提示**：
```java
public class ClassPathResource implements Resource {
    private String path;
    private ClassLoader classLoader;
    
    public ClassPathResource(String path) {
        this.path = path;
        this.classLoader = 获取默认类加载器;
    }
    
    public InputStream getInputStream() throws IOException {
        // 使用类加载器获取资源流
        InputStream is = classLoader.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("资源不存在: " + path);
        }
        return is;
    }
}
```

---

### 6. XmlBeanDefinitionReader - XML配置读取器

**作用**：解析XML配置文件，构建BeanDefinition

**XML配置格式**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean id="helloService" 
          class="com.litespring.demo.service.impl.HelloServiceImpl"
          scope="singleton">
    </bean>
    
    <bean id="userService" 
          class="com.example.UserServiceImpl"
          scope="prototype">
    </bean>
</beans>
```

**解析逻辑**：
```
1. 获取Resource的输入流
2. 使用DOM或SAX解析XML
3. 遍历所有<bean>标签
4. 提取属性：id, class, scope等
5. 创建BeanDefinition对象
6. 注册到BeanDefinitionRegistry
```

**实现提示**：
- 可以使用 `DocumentBuilderFactory` 进行DOM解析
- 获取根元素 `<beans>`
- 获取所有 `<bean>` 子元素
- 读取每个bean的属性
- 创建并注册BeanDefinition

**简化的解析流程伪代码**：
```java
public class XmlBeanDefinitionReader {
    private BeanDefinitionRegistry registry;
    
    public void loadBeanDefinitions(Resource resource) {
        // 1. 获取输入流
        InputStream is = resource.getInputStream();
        
        // 2. 解析XML文档
        Document doc = 解析XML(is);
        
        // 3. 获取根元素
        Element root = doc.getDocumentElement();
        
        // 4. 获取所有bean元素
        NodeList beanNodes = root.getElementsByTagName("bean");
        
        // 5. 遍历处理每个bean
        for (每个bean节点) {
            String id = bean.getAttribute("id");
            String className = bean.getAttribute("class");
            String scope = bean.getAttribute("scope");
            
            // 创建BeanDefinition
            BeanDefinition bd = new BeanDefinition(className);
            if (scope != null) {
                bd.setScope(scope);
            }
            
            // 注册
            registry.registerBeanDefinition(id, bd);
        }
    }
}
```

---

### 7. XmlBeanFactory - 便捷的Bean工厂

**作用**：组合Resource和XmlBeanDefinitionReader，提供便捷的使用方式

**设计**：
```java
public class XmlBeanFactory extends DefaultBeanFactory {
    public XmlBeanFactory(Resource resource) {
        // 创建XML读取器
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);
        
        // 加载Bean定义
        reader.loadBeanDefinitions(resource);
    }
}
```

---

## 实现步骤

### 步骤1：准备工作

**1.1 整理现有代码**
- 查看已有的 `BeanFactory.java`、`BeanDefinition.java` 等
- 可以保留作为参考，也可以删除后自己重写
- 建议：保留接口定义，删除实现类，自己实现

**1.2 创建测试类**
```java
// src/test/java/com/litespring/test/v1/BeanFactoryTest.java
public class BeanFactoryTest {
    // 在这里写测试用例
}
```

**1.3 准备测试用的Bean类**
```java
// 创建一个简单的测试类
package com.litespring.test.v1.service;

public class HelloService {
    public HelloService() {
        System.out.println("HelloService构造函数被调用");
    }
    
    public String sayHello() {
        return "Hello, Lite Spring!";
    }
}
```

---

### 步骤2：实现BeanDefinition

**要求**：
- 能存储Bean的类名
- 能存储Bean的作用域
- 提供getter/setter方法
- 提供isSingleton()和isPrototype()判断方法

**测试用例思路**：
```java
@Test
public void testBeanDefinition() {
    BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
    
    assertEquals("com.litespring.test.v1.service.HelloService", bd.getBeanClassName());
    assertTrue(bd.isSingleton());  // 默认是单例
    
    bd.setScope("prototype");
    assertTrue(bd.isPrototype());
}
```

**关键点**：
- 构造函数需要接收beanClassName参数
- scope默认值是"singleton"
- 提供便捷的判断方法

---

### 步骤3：实现DefaultBeanFactory的基础框架

**要求**：
- 实现BeanDefinitionRegistry接口
- 实现registerBeanDefinition和getBeanDefinition方法
- 暂时不实现getBean方法

**测试用例思路**：
```java
@Test
public void testRegisterBeanDefinition() {
    DefaultBeanFactory factory = new DefaultBeanFactory();
    BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
    
    factory.registerBeanDefinition("helloService", bd);
    
    BeanDefinition bd2 = factory.getBeanDefinition("helloService");
    assertNotNull(bd2);
    assertEquals("com.litespring.test.v1.service.HelloService", bd2.getBeanClassName());
}
```

**关键点**：
- 使用Map存储BeanDefinition
- 注意空指针检查
- Bean名称不存在时抛出异常

---

### 步骤4：实现Bean的创建逻辑

**要求**：
- 实现getBean(String name)方法
- 支持单例Bean缓存
- 使用反射创建Bean实例

**测试用例思路**：
```java
@Test
public void testGetBean() {
    DefaultBeanFactory factory = new DefaultBeanFactory();
    BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
    factory.registerBeanDefinition("helloService", bd);
    
    HelloService service = (HelloService) factory.getBean("helloService");
    assertNotNull(service);
    assertEquals("Hello, Lite Spring!", service.sayHello());
}

@Test
public void testSingletonScope() {
    // 测试单例：多次获取应该是同一个实例
    DefaultBeanFactory factory = new DefaultBeanFactory();
    // ... 注册Bean
    
    Object obj1 = factory.getBean("helloService");
    Object obj2 = factory.getBean("helloService");
    
    assertSame(obj1, obj2);  // 应该是同一个对象
}

@Test
public void testPrototypeScope() {
    // 测试原型：每次获取应该是新实例
    DefaultBeanFactory factory = new DefaultBeanFactory();
    BeanDefinition bd = new BeanDefinition("com.litespring.test.v1.service.HelloService");
    bd.setScope("prototype");
    factory.registerBeanDefinition("helloService", bd);
    
    Object obj1 = factory.getBean("helloService");
    Object obj2 = factory.getBean("helloService");
    
    assertNotSame(obj1, obj2);  // 应该是不同的对象
}
```

**实现关键点**：

1. **getBean方法的实现思路**：
```
getBean(String name) {
    // 1. 获取BeanDefinition
    BeanDefinition bd = getBeanDefinition(name);
    if (bd == null) {
        抛出异常("Bean不存在");
    }
    
    // 2. 如果是单例，检查缓存
    if (bd.isSingleton()) {
        Object bean = singletonObjects.get(name);
        if (bean == null) {
            // 缓存中没有，创建它
            bean = createBean(bd);
            singletonObjects.put(name, bean);
        }
        return bean;
    }
    
    // 3. 如果是原型，每次都创建新的
    return createBean(bd);
}
```

2. **createBean方法的实现思路**：
```
createBean(BeanDefinition bd) {
    // 1. 加载类
    String className = bd.getBeanClassName();
    Class<?> clazz = Class.forName(className);
    
    // 2. 获取无参构造函数
    Constructor<?> constructor = clazz.getConstructor();
    
    // 3. 创建实例
    Object instance = constructor.newInstance();
    
    return instance;
}
```

**注意事项**：
- 需要处理 `ClassNotFoundException`
- 需要处理 `NoSuchMethodException`（没有无参构造函数）
- 需要处理 `InstantiationException`
- 建议定义统一的BeansException来包装这些异常

---

### 步骤5：实现ClassPathResource

**要求**：
- 实现Resource接口
- 能从classpath加载资源
- 提供输入流

**测试用例思路**：
```java
@Test
public void testClassPathResource() throws IOException {
    Resource resource = new ClassPathResource("beans.xml");
    
    InputStream is = resource.getInputStream();
    assertNotNull(is);
    
    // 读取一些内容验证
    // ...
    
    is.close();
}
```

**实现关键点**：
- 使用 `ClassLoader.getResourceAsStream(path)`
- 路径不要以"/"开头（如果使用ClassLoader）
- 资源不存在时抛出 `FileNotFoundException`

---

### 步骤6：实现XmlBeanDefinitionReader

**要求**：
- 能解析XML配置文件
- 提取bean的id、class、scope属性
- 创建并注册BeanDefinition

**准备XML配置文件**：
```xml
<!-- src/test/resources/beans-v1.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean id="helloService" 
          class="com.litespring.test.v1.service.HelloService">
    </bean>
</beans>
```

**测试用例思路**：
```java
@Test
public void testXmlBeanDefinitionReader() {
    DefaultBeanFactory factory = new DefaultBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
    
    Resource resource = new ClassPathResource("beans-v1.xml");
    reader.loadBeanDefinitions(resource);
    
    BeanDefinition bd = factory.getBeanDefinition("helloService");
    assertNotNull(bd);
    assertEquals("com.litespring.test.v1.service.HelloService", bd.getBeanClassName());
}
```

**实现关键点**：

1. **XML解析步骤**：
```java
public void loadBeanDefinitions(Resource resource) {
    InputStream is = null;
    try {
        is = resource.getInputStream();
        
        // 创建DOM解析器
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);
        
        // 获取根元素
        Element root = doc.getDocumentElement();
        
        // 获取所有bean元素
        NodeList beanNodes = root.getElementsByTagName("bean");
        
        // 遍历处理
        for (int i = 0; i < beanNodes.getLength(); i++) {
            Node node = beanNodes.item(i);
            if (node instanceof Element) {
                parseBeanDefinition((Element) node);
            }
        }
    } catch (Exception e) {
        throw new BeansException("解析XML失败", e);
    } finally {
        if (is != null) {
            try { is.close(); } catch (IOException e) { }
        }
    }
}
```

2. **解析单个bean元素**：
```java
private void parseBeanDefinition(Element element) {
    String id = element.getAttribute("id");
    String className = element.getAttribute("class");
    
    BeanDefinition bd = new BeanDefinition(className);
    
    // 解析scope属性（可选）
    if (element.hasAttribute("scope")) {
        bd.setScope(element.getAttribute("scope"));
    }
    
    // 注册
    this.registry.registerBeanDefinition(id, bd);
}
```

**注意事项**：
- 检查id和class属性是否存在
- 处理XML解析异常
- 关闭输入流（使用try-finally或try-with-resources）

---

### 步骤7：实现XmlBeanFactory

**要求**：
- 继承DefaultBeanFactory
- 在构造函数中自动加载XML配置

**测试用例思路**：
```java
@Test
public void testXmlBeanFactory() {
    BeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans-v1.xml"));
    
    HelloService service = (HelloService) factory.getBean("helloService");
    assertNotNull(service);
}
```

**实现关键点**：
- 构造函数接收Resource参数
- 创建XmlBeanDefinitionReader并加载定义
- 非常简单，主要是组合现有组件

---

### 步骤8：完善异常处理

**定义统一的异常类**：
```java
public class BeansException extends RuntimeException {
    public BeansException(String message) {
        super(message);
    }
    
    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**在合适的地方抛出异常**：
- Bean不存在
- 类加载失败
- 实例化失败
- XML解析失败

---

### 步骤9：完善工具类

**ClassUtils工具类**：
```java
public class ClassUtils {
    /**
     * 获取默认的类加载器
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            // 优先使用线程上下文类加载器
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // 无法访问
        }
        
        if (cl == null) {
            // 使用当前类的类加载器
            cl = ClassUtils.class.getClassLoader();
        }
        
        return cl;
    }
}
```

---

## 测试验证

### 综合测试

创建完整的测试场景：

```java
@Test
public void testCompleteScenario() {
    // 1. 准备XML配置
    // 2. 创建工厂
    BeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans-v1.xml"));
    
    // 3. 获取Bean
    HelloService service = (HelloService) factory.getBean("helloService");
    
    // 4. 使用Bean
    String result = service.sayHello();
    
    // 5. 验证
    assertNotNull(service);
    assertEquals("Hello, Lite Spring!", result);
    
    // 6. 验证单例
    HelloService service2 = (HelloService) factory.getBean("helloService");
    assertSame(service, service2);
}
```

### 测试Demo应用

更新 `lite-spring-demo` 中的配置和代码，实际运行：

```java
public class DemoApplication {
    public static void main(String[] args) {
        BeanFactory factory = new XmlBeanFactory(
            new ClassPathResource("beans.xml")
        );
        
        HelloService service = (HelloService) factory.getBean("helloService");
        System.out.println(service.greet("World"));
    }
}
```

---

## 关键难点和解决方案

### 难点1：反射创建对象

**问题**：如何通过类名创建对象？

**解决**：
```java
// 1. 加载类
Class<?> clazz = Class.forName(className);

// 2. 获取构造函数
Constructor<?> constructor = clazz.getDeclaredConstructor();

// 3. 创建实例
Object instance = constructor.newInstance();
```

**注意**：
- 需要处理各种检查异常
- 构造函数可能不存在（没有无参构造）
- 类可能不存在

### 难点2：类加载器的选择

**问题**：使用哪个类加载器？

**解决**：
- 优先使用线程上下文类加载器
- 其次使用当前类的类加载器
- 参考ClassUtils的实现

### 难点3：XML解析

**问题**：如何解析XML？

**解决**：
- 使用Java自带的DOM解析器
- `DocumentBuilderFactory` 和 `DocumentBuilder`
- 也可以使用DOM4J（需要添加依赖）

### 难点4：单例Bean的线程安全

**问题**：并发访问时如何保证单例Bean只创建一次？

**解决**：
- 第一阶段可以使用简单的方式（不考虑并发）
- 后续优化时使用 `synchronized` 或 `ConcurrentHashMap`

---

## 思考题

完成实现后，思考以下问题：

1. **为什么要分BeanDefinition和Bean实例两个概念？**
   - 提示：考虑原型Bean的场景

2. **单例Bean的创建时机是什么？**
   - 提示：懒加载 vs 预加载

3. **如果Bean的类没有无参构造函数怎么办？**
   - 提示：这就是第二阶段要解决的问题（构造器注入）

4. **如果两个Bean互相依赖怎么办？**
   - 提示：这就是第二阶段要解决的循环依赖问题

5. **当前的实现有哪些可以优化的地方？**
   - 性能优化
   - 代码结构优化
   - 错误处理优化

---

## 对比Spring源码

完成后，可以查看Spring的实现：

- `org.springframework.beans.factory.BeanFactory`
- `org.springframework.beans.factory.support.DefaultListableBeanFactory`
- `org.springframework.beans.factory.xml.XmlBeanDefinitionReader`
- `org.springframework.context.support.ClassPathXmlApplicationContext`

**对比要点**：
- Spring的实现更复杂，支持更多特性
- Spring使用了大量的设计模式
- Spring考虑了更多的边界情况和性能优化
- 但核心思想是一致的

---

## 下一阶段预告

第二阶段将实现：
- 属性注入（setter注入）
- 构造器注入
- Bean之间的依赖关系
- 循环依赖的解决（三级缓存）

这会让你的框架更接近真实的Spring！

---

## 常见问题

### Q1: XML解析太复杂了，能简化吗？

A: 可以暂时只解析最基本的属性（id和class），其他属性后续再加。也可以先用硬编码的方式注册Bean，先把Bean创建逻辑跑通。

### Q2: 测试失败了，如何调试？

A: 
1. 使用断点调试，观察变量值
2. 添加System.out.println打印关键信息
3. 检查异常堆栈信息
4. 确认XML配置路径是否正确

### Q3: 反射代码总是出错

A: 
- 检查类的完全限定名是否正确
- 确认类是否有无参构造函数
- 查看具体的异常信息
- 使用try-catch捕获并打印详细错误

### Q4: 需要多长时间完成？

A: 根据个人基础不同，大约需要3-7天：
- 理解理论：1天
- 实现核心功能：2-3天
- 调试和优化：1-2天
- 完善测试：1天

不要急，慢慢来，理解每一行代码的含义。

---

## 检查清单

完成第一阶段后，确认以下内容：

- [ ] BeanDefinition能存储Bean的元数据
- [ ] DefaultBeanFactory能注册和获取BeanDefinition
- [ ] DefaultBeanFactory能创建Bean实例（使用反射）
- [ ] 单例Bean能被正确缓存
- [ ] 原型Bean每次返回新实例
- [ ] ClassPathResource能加载classpath资源
- [ ] XmlBeanDefinitionReader能解析XML配置
- [ ] XmlBeanFactory能从XML创建Bean
- [ ] 异常处理完善
- [ ] 测试用例全部通过
- [ ] Demo应用能正常运行

全部完成后，恭喜你完成了第一阶段！🎉

你已经实现了一个简单但完整的IoC容器，理解了Spring的核心原理！

