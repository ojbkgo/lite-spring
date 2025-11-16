# 第三阶段完成指南

## 🎉 恭喜！第三阶段代码已准备就绪

我已经为你准备好了第三阶段的完整实现，Bean现在有了完整的生命周期！

---

## 📦 已创建的文件

### 核心接口（6个）
1. **InitializingBean** - 初始化Bean接口
   - 位置：`com.litespring.core.InitializingBean`
   - 方法：`afterPropertiesSet()`

2. **DisposableBean** - 可销毁Bean接口
   - 位置：`com.litespring.core.DisposableBean`
   - 方法：`destroy()`

3. **BeanPostProcessor** - Bean后置处理器接口 ⭐核心
   - 位置：`com.litespring.core.BeanPostProcessor`
   - 方法：`postProcessBeforeInitialization()`, `postProcessAfterInitialization()`

4. **Aware** - 标记接口
   - 位置：`com.litespring.core.Aware`

5. **BeanNameAware** - Bean名称感知接口
   - 位置：`com.litespring.core.BeanNameAware`
   - 方法：`setBeanName()`

6. **BeanFactoryAware** - BeanFactory感知接口
   - 位置：`com.litespring.core.BeanFactoryAware`
   - 方法：`setBeanFactory()`

### 核心实现
7. **DefaultBeanFactory_v3** - 增强版Bean工厂 ⭐核心
   - 位置：`com.litespring.core.DefaultBeanFactory_v3`
   - 新增功能：
     - 生命周期管理
     - 初始化方法调用
     - 销毁方法调用
     - BeanPostProcessor支持
     - Aware接口回调
     - 容器关闭（close方法）

### 测试Bean类（5个）
8. **LifecycleBean** - 完整生命周期Bean
   - 实现所有接口，记录回调顺序

9. **SimpleBean** - 简单InitializingBean
   
10. **DisposableTestBean** - 销毁测试Bean
    
11. **CustomInitDestroyBean** - 自定义方法Bean
    
12. **LoggingBeanPostProcessor** - 测试用处理器

### 测试类（5个）
13. **InitializingBeanTest** - 初始化接口测试
14. **DisposableBeanTest** - 销毁接口测试
15. **CustomInitDestroyTest** - 自定义方法测试
16. **BeanPostProcessorTest** - 后置处理器测试
17. **AwareInterfaceTest** - Aware接口测试
18. **FullLifecycleTest** - 完整生命周期测试 ⭐最重要

### XML配置
19. **beans-v3.xml** - 第三阶段配置

---

## 🚀 运行测试

### 运行所有第三阶段测试

```bash
cd /Users/ziyuewen/Devspace/myprj/lite-spring

mvn test -Dtest="com.litespring.test.v3.*"
```

### 运行单个测试

```bash
# 完整生命周期测试（最重要）
mvn test -Dtest=FullLifecycleTest

# 初始化测试
mvn test -Dtest=InitializingBeanTest

# 销毁测试
mvn test -Dtest=DisposableBeanTest

# BeanPostProcessor测试
mvn test -Dtest=BeanPostProcessorTest

# Aware接口测试
mvn test -Dtest=AwareInterfaceTest
```

### 在IDE中运行

特别推荐运行：
- `FullLifecycleTest.testPrintLifecycle()` - 会打印完整的生命周期流程
- `FullLifecycleTest.testCompleteLifecycle()` - 验证调用顺序

---

## 📚 完整的Bean生命周期

### 10个步骤

```
【1】构造函数
     ↓
【2】属性注入（Setter方法）
     ↓
【3】BeanNameAware.setBeanName()
     ↓
【4】BeanFactoryAware.setBeanFactory()
     ↓
【5】BeanPostProcessor.postProcessBeforeInitialization() ← 前置处理
     ↓
【6】InitializingBean.afterPropertiesSet()
     ↓
【7】自定义init-method
     ↓
【8】BeanPostProcessor.postProcessAfterInitialization() ← 后置处理（AOP在这里）
     ↓
【9】Bean就绪，可以使用
     ↓
【容器关闭】
     ↓
【10】DisposableBean.destroy()
     ↓
【11】自定义destroy-method
```

### 执行结果示例

运行 `FullLifecycleTest.testPrintLifecycle()`，你会看到：

```
========== 开始创建Bean ==========
【1】LifecycleBean构造函数被调用
【2】属性注入: property = test-value
【3】BeanNameAware.setBeanName: lifecycleBean
【4】BeanFactoryAware.setBeanFactory
【BeanPostProcessor-前置】处理Bean: lifecycleBean
【5】InitializingBean.afterPropertiesSet
【6】customInit方法被调用
【BeanPostProcessor-后置】处理Bean: lifecycleBean

========== Bean创建完成 ==========
回调顺序: [1.构造函数, 2.属性注入, 3.BeanNameAware, 4.BeanFactoryAware, 5.InitializingBean, 6.customInit]

========== 关闭容器 ==========
【7】DisposableBean.destroy
【8】customDestroy方法被调用

========== 容器已关闭 ==========
完整回调: [1.构造函数, 2.属性注入, 3.BeanNameAware, 4.BeanFactoryAware, 5.InitializingBean, 6.customInit, 7.DisposableBean, 8.customDestroy]
```

---

## 🎯 核心代码解析

### 1. initializeBean方法（核心）

这是第三阶段最核心的方法：

```java
private Object initializeBean(String beanName, Object bean, BeanDefinition bd) {
    // 【1】调用Aware接口
    invokeAwareMethods(beanName, bean);
    
    // 【2】BeanPostProcessor前置处理
    Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
    
    // 【3】调用初始化方法
    invokeInitMethods(beanName, wrappedBean, bd);
    
    // 【4】BeanPostProcessor后置处理
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    
    // 【5】注册销毁回调
    registerDisposableBeanIfNecessary(beanName, wrappedBean, bd);
    
    return wrappedBean;
}
```

**调用时机**：在 `createBean` 方法中，属性注入之后调用

### 2. invokeAwareMethods方法

```java
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof BeanNameAware) {
        ((BeanNameAware) bean).setBeanName(beanName);
    }
    
    if (bean instanceof BeanFactoryAware) {
        ((BeanFactoryAware) bean).setBeanFactory(this);
    }
}
```

**关键**：使用 `instanceof` 判断Bean是否实现了接口

### 3. invokeInitMethods方法

```java
private void invokeInitMethods(String beanName, Object bean, BeanDefinition bd) {
    // 先调用InitializingBean接口
    if (bean instanceof InitializingBean) {
        ((InitializingBean) bean).afterPropertiesSet();
    }
    
    // 再调用自定义init-method
    String initMethodName = bd.getInitMethodName();
    if (initMethodName != null && !initMethodName.isEmpty()) {
        // 避免重复调用
        if (bean instanceof InitializingBean && "afterPropertiesSet".equals(initMethodName)) {
            return;
        }
        
        Method initMethod = bean.getClass().getMethod(initMethodName);
        initMethod.invoke(bean);
    }
}
```

**关键**：
- 两种初始化方式都支持
- 避免重复调用（如果方法名相同）

### 4. BeanPostProcessor的应用

```java
private Object applyBeanPostProcessorsAfterInitialization(Object bean, String beanName) {
    Object result = bean;
    
    // 遍历所有BeanPostProcessor
    for (BeanPostProcessor processor : getBeanPostProcessors()) {
        Object current = processor.postProcessAfterInitialization(result, beanName);
        
        // 如果返回null，使用原Bean
        if (current == null) {
            return result;
        }
        
        // 使用处理后的Bean（可能是代理对象）
        result = current;
    }
    
    return result;
}
```

**关键**：
- 可能返回不同的对象（如AOP代理）
- 支持多个BeanPostProcessor链式处理

### 5. 容器关闭和销毁

```java
public void close() {
    // 逆序销毁
    List<String> beanNames = new ArrayList<>(disposableBeans.keySet());
    Collections.reverse(beanNames);
    
    for (String beanName : beanNames) {
        try {
            destroyBean(beanName);
        } catch (Exception e) {
            // 销毁失败不影响其他Bean
            System.err.println("销毁Bean失败: " + beanName);
        }
    }
}
```

**关键**：
- 逆序销毁（后创建的先销毁）
- 异常处理（一个失败不影响其他）

---

## 🎯 与第二阶段的对比

| 功能 | 第二阶段 | 第三阶段 |
|------|---------|---------|
| **Bean创建** | ✅ 实例化+属性注入 | ✅ +完整生命周期 |
| **初始化回调** | ❌ 不支持 | ✅ 接口+配置 |
| **销毁回调** | ❌ 不支持 | ✅ 接口+配置 |
| **BeanPostProcessor** | ❌ 无 | ✅ 完整支持 |
| **Aware接口** | ❌ 无 | ✅ 2个接口 |
| **容器关闭** | ❌ 无 | ✅ close()方法 |

---

## ✅ 验证清单

完成第三阶段后，确认以下功能：

- [ ] InitializingBean.afterPropertiesSet()被正确调用
- [ ] 自定义init-method被正确调用
- [ ] DisposableBean.destroy()在容器关闭时被调用
- [ ] 自定义destroy-method在容器关闭时被调用
- [ ] BeanNameAware.setBeanName()被调用
- [ ] BeanFactoryAware.setBeanFactory()被调用
- [ ] BeanPostProcessor前置处理被调用
- [ ] BeanPostProcessor后置处理被调用
- [ ] 生命周期回调顺序正确
- [ ] 销毁顺序是逆序（后创建先销毁）
- [ ] 避免重复调用（接口方法和配置方法名相同时）
- [ ] 所有测试通过

---

## 🎓 学习要点

### 1. Bean生命周期的完整流程

查看 `LifecycleBean` 类，它实现了所有接口，运行测试观察输出：
- 每个回调的调用时机
- 回调的顺序
- 哪些是可选的，哪些是必须的

### 2. BeanPostProcessor的威力

理解为什么它是Spring最重要的扩展点：
- 可以在初始化前后插入逻辑
- 可以替换Bean（返回不同的对象）
- AOP代理就是在后置处理中创建的

### 3. 两种初始化方式的对比

| 方式 | 侵入性 | 灵活性 | 推荐度 |
|-----|--------|-------|--------|
| **InitializingBean接口** | 侵入式 | 固定方法名 | ⭐⭐⭐ |
| **init-method配置** | 非侵入式 | 自定义方法名 | ⭐⭐⭐⭐ |
| **@PostConstruct注解** | 轻侵入 | 方便 | ⭐⭐⭐⭐⭐ |

### 4. 为什么需要Aware接口？

```java
public class MyService implements BeanFactoryAware {
    private BeanFactory beanFactory;
    
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    public void doSomething() {
        // 可以动态获取其他Bean
        OtherBean other = beanFactory.getBean("otherBean", OtherBean.class);
    }
}
```

**应用场景**：
- 需要动态获取Bean
- 需要知道自己的名称
- 需要访问ApplicationContext

---

## 🔍 关键方法调用链

### 创建Bean的完整调用链

```
getBean("lifecycleBean")
  ↓
doGetBean("lifecycleBean")
  ↓
getSingleton("lifecycleBean", factory)
  ↓
createBean("lifecycleBean", bd)
  ↓
  instantiateBean(bd)  ← 【1】实例化
  ↓
  populateBean(...)    ← 【2】属性注入
  ↓
  initializeBean(...)  ← 【3】初始化（第三阶段新增）
    ↓
    invokeAwareMethods(...)  ← 3.1 Aware接口
    ↓
    applyBeanPostProcessorsBeforeInitialization(...)  ← 3.2 前置处理
    ↓
    invokeInitMethods(...)  ← 3.3 初始化方法
      ↓
      InitializingBean.afterPropertiesSet()
      ↓
      自定义init-method
    ↓
    applyBeanPostProcessorsAfterInitialization(...)  ← 3.4 后置处理
    ↓
    registerDisposableBeanIfNecessary(...)  ← 3.5 注册销毁回调
  ↓
返回Bean
```

### 容器关闭的调用链

```
factory.close()
  ↓
遍历disposableBeans
  ↓
  destroyBean(beanName)
    ↓
    DisposableBean.destroy()
    ↓
    自定义destroy-method
```

---

## 🧪 测试用例说明

### FullLifecycleTest.testCompleteLifecycle（最重要）

这个测试验证了完整的生命周期：

**初始化阶段验证**：
```java
List<String> callbacks = bean.getCallbacks();

assertEquals("1.构造函数", callbacks.get(0));
assertEquals("2.属性注入", callbacks.get(1));
assertEquals("3.BeanNameAware", callbacks.get(2));
assertEquals("4.BeanFactoryAware", callbacks.get(3));
assertEquals("5.InitializingBean", callbacks.get(4));
assertEquals("6.customInit", callbacks.get(5));
```

**销毁阶段验证**：
```java
factory.close();

assertEquals("7.DisposableBean", callbacks.get(6));
assertEquals("8.customDestroy", callbacks.get(7));
```

### FullLifecycleTest.testPrintLifecycle（观察流程）

运行这个测试，在控制台查看完整的输出，直观理解生命周期。

---

## 💡 代码学习要点

### 1. 理解BeanPostProcessor的返回值

```java
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // 可以返回原Bean
    return bean;
    
    // 也可以返回代理对象（AOP就是这样实现的）
    return createProxy(bean);
    
    // 还可以返回包装后的Bean
    return new BeanWrapper(bean);
}
```

**为什么可以返回不同对象？**
- 这就是装饰器模式
- 允许在不修改原Bean的情况下增强功能
- AOP代理就是利用这个机制

### 2. 理解避免重复调用的逻辑

```java
// 如果接口方法名和配置方法名相同，只调用一次
if (bean instanceof InitializingBean && "afterPropertiesSet".equals(initMethodName)) {
    return;  // 跳过，避免重复调用
}
```

### 3. 理解销毁的逆序处理

```java
List<String> beanNames = new ArrayList<>(disposableBeans.keySet());
Collections.reverse(beanNames);  // 逆序
```

**为什么逆序？**
- A依赖B，A后创建
- 销毁时应该先销毁A，再销毁B
- 就像"后进先出"的栈

---

## 🎯 重要概念

### BeanPostProcessor的两个时机

**前置处理（Before）**：
- 在InitializingBean和init-method之前
- 可以修改Bean的状态
- 应用：属性验证、依赖检查

**后置处理（After）**：
- 在InitializingBean和init-method之后
- 可以替换Bean（返回代理）
- 应用：AOP代理创建 ⭐

### Aware接口的设计

**为什么叫Aware（感知）？**
- Bean"感知"到自己的名字
- Bean"感知"到容器的存在
- 是一种回调机制

**何时使用？**
- 需要动态获取其他Bean
- 需要知道自己在容器中的名称
- 需要访问容器的功能

---

## 🤔 思考题回顾

### 1. 为什么需要InitializingBean和init-method两种方式？

**InitializingBean接口**：
- ✅ 不需要配置
- ✅ 编译期检查
- ❌ 侵入式（依赖Spring）

**init-method配置**：
- ✅ 非侵入式
- ✅ 方法名自定义
- ❌ 需要XML配置

**两者结合**：灵活性最大

### 2. BeanPostProcessor返回值的含义？

**返回null**：
- 停止后续处理
- 使用当前的Bean

**返回bean**：
- 继续后续处理
- 不改变Bean

**返回其他对象**：
- 替换Bean
- 如AOP代理

### 3. 为什么AOP在postProcessAfterInitialization创建？

**答案**：
- 确保Bean完全初始化后再创建代理
- 代理对象包装的是完整的Bean
- 如果在前面创建，Bean的初始化逻辑就丢失了

### 4. 原型Bean需要销毁回调吗？

**答案**：不需要
- 原型Bean每次都创建新的
- 容器不管理原型Bean的生命周期
- 用户自己负责销毁

**代码体现**：
```java
if (!bd.isSingleton()) {
    return;  // 只有单例才注册销毁
}
```

---

## 📊 代码统计

| 类型 | 数量 | 说明 |
|------|------|------|
| **接口** | 6个 | 生命周期接口 |
| **核心实现** | 1个 | DefaultBeanFactory_v3 |
| **测试Bean** | 5个 | 各种场景的测试类 |
| **测试类** | 5个 | 约25个测试用例 |
| **新增代码** | ~400行 | 生命周期管理代码 |

---

## 🚀 下一步

### 完成第三阶段后

1. **运行测试**
   ```bash
   mvn test -Dtest="com.litespring.test.v3.*"
   ```

2. **阅读代码**（2-3小时）
   - DefaultBeanFactory_v3.initializeBean()
   - LifecycleBean的实现
   - 各个测试类

3. **调试观察**（1小时）
   - 在initializeBean打断点
   - 观察回调顺序
   - 查看控制台输出

4. **准备第四阶段**
   - 第四阶段：注解驱动开发
   - @Component、@Autowired等注解
   - 摆脱XML配置

---

## 🎊 你现在掌握了

- ✅ Bean的完整生命周期（10个步骤）
- ✅ InitializingBean和DisposableBean接口
- ✅ 自定义初始化和销毁方法
- ✅ BeanPostProcessor扩展点（Spring最核心的扩展机制）
- ✅ Aware接口的作用和使用
- ✅ 容器的关闭和资源清理

**第三阶段让Bean真正"活"起来了！** 

从现在开始，你的框架已经具备了Spring IoC容器的核心功能！💪

---

准备好运行测试和学习代码了吗？有任何问题随时问我！🚀

