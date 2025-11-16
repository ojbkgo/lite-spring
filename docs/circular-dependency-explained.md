# 循环依赖解决方案详解

## 📚 目录

1. [问题场景](#问题场景)
2. [三级缓存数据结构](#三级缓存数据结构)
3. [核心方法解析](#核心方法解析)
4. [完整执行流程](#完整执行流程)
5. [代码跟踪](#代码跟踪)
6. [为什么需要三级缓存](#为什么需要三级缓存)
7. [无法解决的情况](#无法解决的情况)

---

## 🎯 问题场景

### 循环依赖示例

```java
// A依赖B
public class CircularA {
    private CircularB circularB;
    
    public void setCircularB(CircularB circularB) {
        this.circularB = circularB;
    }
}

// B依赖A
public class CircularB {
    private CircularA circularA;
    
    public void setCircularA(CircularA circularA) {
        this.circularA = circularA;
    }
}
```

### XML配置

```xml
<bean id="circularA" class="com.litespring.test.v2.CircularA">
    <property name="circularB" ref="circularB"/>
</bean>

<bean id="circularB" class="com.litespring.test.v2.CircularB">
    <property name="circularA" ref="circularA"/>
</bean>
```

### 问题分析

如果没有特殊处理，会发生什么？

```
1. 创建A → 需要注入B
2. 创建B → 需要注入A
3. 创建A → 需要注入B
4. 创建B → 需要注入A
5. ... 无限循环，栈溢出！
```

---

## 🗄️ 三级缓存数据结构

### 代码定义

```java
public class DefaultBeanFactory_v2 {
    
    /**
     * 一级缓存：singletonObjects
     * 存储完全初始化好的单例Bean（成品）
     * key: beanName
     * value: 完整的Bean实例
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    /**
     * 二级缓存：earlySingletonObjects
     * 存储早期暴露的Bean（半成品）
     * 已经实例化，但还没有完成属性注入
     * key: beanName
     * value: Bean的早期引用
     */
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    
    /**
     * 三级缓存：singletonFactories
     * 存储对象工厂（用于延迟创建代理）
     * key: beanName
     * value: 创建Bean早期引用的工厂
     */
    private final Map<String, ObjectFactory> singletonFactories = new HashMap<>();
    
    /**
     * 正在创建的Bean集合
     * 用于检测循环依赖
     */
    private final Set<String> singletonsCurrentlyInCreation = 
        Collections.newSetFromMap(new ConcurrentHashMap<>());
}
```

### 三级缓存的比喻

**一级缓存（singletonObjects）**：
- 就像**成品仓库**
- 存放完全组装好的成品
- 可以直接拿来使用

**二级缓存（earlySingletonObjects）**：
- 就像**半成品仓库**
- 存放已经有了外壳，但还没装配好的产品
- 可以拿来引用，但功能还不完整

**三级缓存（singletonFactories）**：
- 就像**生产线**
- 存放的是"生产半成品的方法"
- 只有真正需要时才执行生产

---

## 🔧 核心方法解析

### 方法1：doGetBean - 获取Bean入口

```java
/**
 * 获取Bean的核心方法（支持三级缓存）
 */
private Object doGetBean(String beanName) {
    // 【步骤1】尝试从缓存获取（三级缓存依次查找）
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null) {
        return sharedInstance;  // 缓存中有，直接返回
    }
    
    // 【步骤2】缓存中没有，需要创建
    BeanDefinition bd = beanDefinitions.get(beanName);
    if (bd == null) {
        throw new BeansException("Bean不存在: " + beanName);
    }
    
    // 【步骤3】单例Bean的创建
    if (bd.isSingleton()) {
        // 检测构造器循环依赖（无法解决的情况）
        if (bd.hasConstructorArgumentValues() && isSingletonCurrentlyInCreation(beanName)) {
            throw new BeansException("检测到构造器循环依赖，无法解决: " + beanName);
        }
        
        // 创建单例Bean（带工厂方法）
        sharedInstance = getSingleton(beanName, () -> {
            return createBean(beanName, bd);
        });
        return sharedInstance;
    }
    
    // ... 原型Bean的处理 ...
}
```

**关键点**：
- 先查缓存，缓存有就直接返回
- 缓存没有才创建
- 使用 `getSingleton(beanName, factory)` 创建Bean

---

### 方法2：getSingleton(String) - 三级缓存查找

```java
/**
 * 从三级缓存中获取Bean
 * 这是解决循环依赖的核心方法
 */
private Object getSingleton(String beanName) {
    // 【第一步】从一级缓存获取（成品仓库）
    Object singletonObject = singletonObjects.get(beanName);
    
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // Bean不在一级缓存，但正在创建中（说明可能有循环依赖）
        
        synchronized (this.singletonObjects) {
            // 【第二步】从二级缓存获取（半成品仓库）
            singletonObject = earlySingletonObjects.get(beanName);
            
            if (singletonObject == null) {
                // 【第三步】从三级缓存获取工厂（生产线）
                ObjectFactory singletonFactory = singletonFactories.get(beanName);
                
                if (singletonFactory != null) {
                    // 通过工厂创建早期引用
                    singletonObject = singletonFactory.getObject();
                    
                    // 【升级】从三级缓存升级到二级缓存
                    earlySingletonObjects.put(beanName, singletonObject);
                    singletonFactories.remove(beanName);
                }
            }
        }
    }
    
    return singletonObject;
}
```

**三级查找流程**：
```
查找顺序：
1. 一级缓存（singletonObjects） → 找到就返回完整Bean
   ↓ 没找到
2. 检查是否正在创建 → 不是正在创建就返回null
   ↓ 是正在创建
3. 二级缓存（earlySingletonObjects） → 找到就返回早期引用
   ↓ 没找到
4. 三级缓存（singletonFactories） → 调用工厂创建早期引用
   ↓
5. 升级到二级缓存，删除三级缓存
```

---

### 方法3：getSingleton(String, ObjectFactory) - 创建Bean

```java
/**
 * 获取单例Bean（带工厂）
 * 负责创建Bean并管理缓存
 */
private Object getSingleton(String beanName, ObjectFactory objectFactory) {
    synchronized (this.singletonObjects) {
        // 再次检查一级缓存
        Object singletonObject = singletonObjects.get(beanName);
        
        if (singletonObject == null) {
            // 【标记】标记为"正在创建"
            beforeSingletonCreation(beanName);
            
            try {
                // 【创建】调用工厂方法创建Bean
                singletonObject = objectFactory.getObject();
                // 这里会调用 createBean(beanName, bd)
                
                // 【升级到一级缓存】Bean创建完成，放入一级缓存
                earlySingletonObjects.remove(beanName);  // 从二级移除
                singletonFactories.remove(beanName);     // 从三级移除
                singletonObjects.put(beanName, singletonObject);  // 放入一级
                
            } catch (Exception e) {
                throw new BeansException("创建Bean失败: " + beanName, e);
            } finally {
                // 【取消标记】移除"正在创建"标记
                afterSingletonCreation(beanName);
            }
        }
        
        return singletonObject;
    }
}

// 标记Bean正在创建
private void beforeSingletonCreation(String beanName) {
    if (!singletonsCurrentlyInCreation.add(beanName)) {
        throw new BeansException("Bean正在创建中: " + beanName);
    }
}

// 取消正在创建标记
private void afterSingletonCreation(String beanName) {
    if (!singletonsCurrentlyInCreation.remove(beanName)) {
        throw new IllegalStateException("Singleton " + beanName + " 不在创建中");
    }
}
```

**关键点**：
- 创建Bean前：标记为"正在创建"
- 创建Bean：调用工厂方法（会执行createBean）
- 创建完成：放入一级缓存，清理二三级缓存
- 最后：移除"正在创建"标记

---

### 方法4：createBean - Bean创建核心

```java
/**
 * 创建Bean实例
 * 这里实现了"提前暴露"的关键逻辑
 */
private Object createBean(String beanName, BeanDefinition bd) {
    // 【步骤1】实例化Bean（此时属性都是默认值）
    Object bean = instantiateBean(bd);
    
    // 【步骤2】提前暴露：放入三级缓存
    // 这是解决循环依赖的关键！
    if (bd.isSingleton() && !bd.hasConstructorArgumentValues()) {
        addSingletonFactory(beanName, () -> bean);
        // 注意：这里传入的是lambda，是一个ObjectFactory
        // 只有真正需要时才会调用
    }
    
    // 【步骤3】属性注入（可能触发循环依赖）
    populateBean(beanName, bean, bd);
    
    return bean;
}

/**
 * 添加到三级缓存
 */
private void addSingletonFactory(String beanName, ObjectFactory objectFactory) {
    synchronized (this.singletonObjects) {
        if (!singletonObjects.containsKey(beanName)) {
            singletonFactories.put(beanName, objectFactory);
            earlySingletonObjects.remove(beanName);
        }
    }
}
```

**关键理解**：
- 实例化后立即放入三级缓存
- **此时Bean还是"半成品"**（属性未注入）
- 但已经有了对象引用，可以被其他Bean依赖

---

### 方法5：populateBean - 属性注入

```java
/**
 * 填充Bean属性（Setter注入）
 * 这里会递归调用getBean，可能触发循环依赖
 */
private void populateBean(String beanName, Object bean, BeanDefinition bd) {
    PropertyValues pvs = bd.getPropertyValues();
    if (pvs.isEmpty()) {
        return;
    }
    
    // 遍历所有属性
    for (PropertyValue pv : pvs.getPropertyValues()) {
        String propertyName = pv.getName();
        Object value = pv.getValue();
        
        // 【关键】解析属性值
        Object resolvedValue = resolveValueIfNecessary(value);
        
        // 通过setter方法设置属性
        // ... 省略setter调用代码 ...
    }
}

/**
 * 解析属性值
 * 如果是Bean引用，会递归调用getBean
 */
private Object resolveValueIfNecessary(Object value) {
    if (value instanceof RuntimeBeanReference) {
        // 【递归调用】获取依赖的Bean
        RuntimeBeanReference ref = (RuntimeBeanReference) value;
        return getBean(ref.getBeanName());  // ← 这里会触发循环依赖
    } else if (value instanceof TypedStringValue) {
        return ((TypedStringValue) value).getValue();
    }
    return value;
}
```

**触发循环依赖的地方**：
- 在属性注入时调用 `getBean(依赖的BeanName)`
- 如果依赖的Bean又依赖回来，就形成了循环

---

## 🎬 完整执行流程

### 场景：A依赖B，B依赖A

让我们逐步跟踪代码执行：

#### 初始状态

```
一级缓存（singletonObjects）：     {}
二级缓存（earlySingletonObjects）： {}
三级缓存（singletonFactories）：    {}
正在创建（singletonsCurrentlyInCreation）：{}
```

---

#### 第1步：调用 `getBean("circularA")`

**进入 doGetBean("circularA")**

```java
// 1. 尝试从缓存获取
Object sharedInstance = getSingleton("circularA");
// 返回null（缓存都是空的）

// 2. 创建单例Bean
sharedInstance = getSingleton("circularA", () -> {
    return createBean("circularA", bd);
});
```

**进入 getSingleton("circularA", factory)**

```java
// 标记为正在创建
beforeSingletonCreation("circularA");
// singletonsCurrentlyInCreation.add("circularA")

// 调用工厂方法
singletonObject = objectFactory.getObject();
// 即：createBean("circularA", bd)
```

**状态更新**：
```
正在创建：{"circularA"}
```

---

#### 第2步：createBean("circularA")

```java
// 1. 实例化A（new CircularA()）
Object beanA = instantiateBean(bd);
// 此时：beanA = CircularA@001 (circularB属性为null)

// 2. 提前暴露到三级缓存
addSingletonFactory("circularA", () -> beanA);

// 3. 属性注入
populateBean("circularA", beanA, bd);
```

**状态更新**：
```
一级缓存：{}
二级缓存：{}
三级缓存：{"circularA" -> ObjectFactory(返回CircularA@001)}
正在创建：{"circularA"}
```

**关键**：CircularA@001已经存在三级缓存中了！

---

#### 第3步：populateBean("circularA") - 注入B

```java
// 遍历属性，发现需要注入circularB
Object resolvedValue = resolveValueIfNecessary(
    new RuntimeBeanReference("circularB")
);

// 递归调用
return getBean("circularB");  // ← 开始创建B
```

**发起新的调用**：`getBean("circularB")`

---

#### 第4步：创建B - doGetBean("circularB")

```java
// 1. 尝试从缓存获取
Object sharedInstance = getSingleton("circularB");
// 返回null（B还没创建）

// 2. 创建B
sharedInstance = getSingleton("circularB", () -> {
    return createBean("circularB", bd);
});
```

**进入 getSingleton("circularB", factory)**

```java
// 标记B正在创建
beforeSingletonCreation("circularB");

// 调用createBean("circularB")
```

**状态更新**：
```
正在创建：{"circularA", "circularB"}
```

---

#### 第5步：createBean("circularB")

```java
// 1. 实例化B（new CircularB()）
Object beanB = instantiateBean(bd);
// 此时：beanB = CircularB@002 (circularA属性为null)

// 2. 提前暴露到三级缓存
addSingletonFactory("circularB", () -> beanB);

// 3. 属性注入
populateBean("circularB", beanB, bd);
```

**状态更新**：
```
一级缓存：{}
二级缓存：{}
三级缓存：{
    "circularA" -> ObjectFactory(返回CircularA@001),
    "circularB" -> ObjectFactory(返回CircularB@002)
}
正在创建：{"circularA", "circularB"}
```

---

#### 第6步：populateBean("circularB") - 注入A（循环依赖触发）

```java
// 遍历属性，发现需要注入circularA
Object resolvedValue = resolveValueIfNecessary(
    new RuntimeBeanReference("circularA")
);

// 递归调用 ← 这里就是循环依赖！
return getBean("circularA");
```

**关键时刻**：B需要A，但A还没完成创建！

---

#### 第7步：再次 getBean("circularA") - 循环依赖解决

**进入 doGetBean("circularA")**

```java
// 尝试从缓存获取
Object sharedInstance = getSingleton("circularA");
```

**进入 getSingleton("circularA") - 三级缓存查找**

```java
// 【第一步】从一级缓存获取
Object singletonObject = singletonObjects.get("circularA");
// 返回null（A还没完成创建）

// 【判断】A正在创建中吗？
if (singletonObject == null && isSingletonCurrentlyInCreation("circularA")) {
    // 是的！circularA在正在创建集合中
    
    synchronized (this.singletonObjects) {
        // 【第二步】从二级缓存获取
        singletonObject = earlySingletonObjects.get("circularA");
        // 返回null（还没升级到二级缓存）
        
        if (singletonObject == null) {
            // 【第三步】从三级缓存获取工厂
            ObjectFactory factory = singletonFactories.get("circularA");
            // 找到了！factory不为null
            
            if (factory != null) {
                // 【调用工厂】创建早期引用
                singletonObject = factory.getObject();
                // 返回：CircularA@001
                
                // 【升级到二级缓存】
                earlySingletonObjects.put("circularA", singletonObject);
                singletonFactories.remove("circularA");
            }
        }
    }
}

return singletonObject;  // 返回 CircularA@001
```

**状态更新**：
```
一级缓存：{}
二级缓存：{"circularA" -> CircularA@001}  ← A升级到二级缓存
三级缓存：{"circularB" -> ObjectFactory(返回CircularB@002)}  ← A移除
正在创建：{"circularA", "circularB"}
```

**关键**：
- 从三级缓存获取到了A的早期引用（CircularA@001）
- 虽然A的属性还没注入完成，但对象引用已存在
- B可以拿到这个引用并注入

---

#### 第8步：B完成属性注入

```java
// populateBean("circularB") 继续执行
// 成功获取到了A的引用（CircularA@001）
// 通过setter设置
beanB.setCircularA(CircularA@001);

// populateBean完成，createBean("circularB")返回
return beanB;  // 返回 CircularB@002（circularA已注入）
```

**B创建完成**！

---

#### 第9步：B放入一级缓存

**回到 getSingleton("circularB", factory)**

```java
// createBean完成，返回了CircularB@002

// 【升级到一级缓存】
earlySingletonObjects.remove("circularB");
singletonFactories.remove("circularB");
singletonObjects.put("circularB", CircularB@002);

// 移除正在创建标记
afterSingletonCreation("circularB");

return CircularB@002;
```

**状态更新**：
```
一级缓存：{"circularB" -> CircularB@002}  ← B完成
二级缓存：{"circularA" -> CircularA@001}
三级缓存：{}
正在创建：{"circularA"}  ← B移除
```

---

#### 第10步：A完成属性注入

**回到 populateBean("circularA")**

```java
// 成功获取到了B（CircularB@002）
// 通过setter设置
beanA.setCircularB(CircularB@002);

// populateBean完成，createBean("circularA")返回
return beanA;  // 返回 CircularA@001（circularB已注入）
```

**A创建完成**！

---

#### 第11步：A放入一级缓存

**回到 getSingleton("circularA", factory)**

```java
// createBean完成，返回了CircularA@001

// 【升级到一级缓存】
earlySingletonObjects.remove("circularA");  // 从二级移除
singletonFactories.remove("circularA");
singletonObjects.put("circularA", CircularA@001);  // 放入一级

// 移除正在创建标记
afterSingletonCreation("circularA");

return CircularA@001;
```

**最终状态**：
```
一级缓存：{
    "circularA" -> CircularA@001（circularB已注入）,
    "circularB" -> CircularB@002（circularA已注入）
}
二级缓存：{}
三级缓存：{}
正在创建：{}
```

**完成**！A和B都创建好了，互相引用正确！

---

## 🎨 流程图总结

```
getBean("A")
    ↓
查一级缓存 → 没有
    ↓
标记A正在创建
    ↓
createBean("A")
    ↓
实例化A（new A）
    ↓
A放入三级缓存（工厂）
    ↓
属性注入A
    ↓
需要B → getBean("B")
            ↓
        查一级缓存 → 没有
            ↓
        标记B正在创建
            ↓
        createBean("B")
            ↓
        实例化B（new B）
            ↓
        B放入三级缓存
            ↓
        属性注入B
            ↓
        需要A → getBean("A")  ← 循环依赖！
                    ↓
                查一级缓存 → 没有
                    ↓
                A正在创建？→ 是
                    ↓
                查二级缓存 → 没有
                    ↓
                查三级缓存 → 找到A的工厂
                    ↓
                调用工厂获取A
                    ↓
                A升级到二级缓存
                    ↓
                返回A的早期引用 ← B拿到了A！
            ↓
        B完成属性注入
            ↓
        B放入一级缓存
            ↓
        返回B ← A拿到了B！
    ↓
A完成属性注入
    ↓
A放入一级缓存
    ↓
完成！
```

---

## 💡 为什么需要三级缓存？

### 为什么不是两级？

**两级缓存的假设**：
```java
// 一级：完整Bean
Map<String, Object> singletonObjects;

// 二级：早期引用
Map<String, Object> earlySingletonObjects;
```

**问题**：如果Bean需要AOP代理怎么办？

```java
@Transactional  // 需要创建代理
public class UserService {
    @Autowired
    private OrderService orderService;
}
```

**两级缓存的困境**：
1. 创建UserService实例后，立即放入二级缓存
2. 但什么时候创建代理呢？
   - 如果立即创建代理：浪费性能（可能没有循环依赖）
   - 如果延迟创建代理：但已经放入缓存了，引用已暴露

**三级缓存的优势**：
```java
// 三级缓存存储的是工厂
singletonFactories.put("userService", () -> {
    // 只有在真正需要时才决定是否创建代理
    if (needsProxy(userService)) {
        return createProxy(userService);  // 创建代理
    } else {
        return userService;  // 返回原始对象
    }
});
```

**延迟决策**：
- 没有循环依赖：不会调用工厂，不创建代理（性能优化）
- 有循环依赖：调用工厂，根据需要创建代理（灵活性）

### 三级缓存的真实场景

```java
// 没有循环依赖的情况
getBean("userService")
    ↓
实例化 → 放入三级缓存
    ↓
属性注入（没有循环依赖）
    ↓
初始化（BeanPostProcessor创建代理）← 在这里创建代理
    ↓
放入一级缓存
    ↓
三级缓存的工厂从未被调用！

// 有循环依赖的情况
getBean("A")
    ↓
实例化A → 放入三级缓存（工厂）
    ↓
属性注入A → 需要B
    ↓
创建B → 需要A
    ↓
从三级缓存获取A → 调用工厂  ← 这时才创建代理
    ↓
B拿到A的代理
    ↓
A拿到B
    ↓
完成
```

---

## ❌ 无法解决的情况

### 1. 构造器循环依赖

```java
public class A {
    public A(B b) {  // 构造器需要B
        this.b = b;
    }
}

public class B {
    public B(A a) {  // 构造器需要A
        this.a = a;
    }
}
```

**为什么无法解决？**

```
创建A：new A(b) → 需要b实例
创建B：new B(a) → 需要a实例
创建A：new A(b) → 需要b实例
... 死循环

问题：实例都还没创建，怎么提前暴露？
```

**代码检测**：

```java
if (bd.hasConstructorArgumentValues() && isSingletonCurrentlyInCreation(beanName)) {
    throw new BeansException("检测到构造器循环依赖，无法解决: " + beanName);
}
```

### 2. 原型Bean循环依赖

```java
<bean id="a" class="A" scope="prototype">
    <property name="b" ref="b"/>
</bean>

<bean id="b" class="B" scope="prototype">
    <property name="a" ref="a"/>
</bean>
```

**为什么无法解决？**

```
原型Bean不缓存，每次都创建新实例

getBean("a") → 创建新的A实例
    ↓
需要B → getBean("b") → 创建新的B实例
    ↓
需要A → getBean("a") → 又创建新的A实例
    ↓
需要B → getBean("b") → 又创建新的B实例
    ↓
... 无限循环
```

**代码检测**：

```java
// 使用ThreadLocal检测
private final ThreadLocal<Set<String>> prototypesCurrentlyInCreation = 
    ThreadLocal.withInitial(HashSet::new);

private void beforePrototypeCreation(String beanName) {
    Set<String> creating = prototypesCurrentlyInCreation.get();
    if (!creating.add(beanName)) {
        throw new BeansException("检测到原型Bean的循环依赖: " + beanName);
    }
}
```

---

## 🎯 关键要点总结

### 三级缓存的作用

1. **一级缓存（singletonObjects）**
   - 存储完整的Bean
   - 属性已注入，初始化已完成
   - 可以直接使用

2. **二级缓存（earlySingletonObjects）**
   - 存储早期引用
   - 已实例化，但属性未注入
   - 用于打破循环引用

3. **三级缓存（singletonFactories）**
   - 存储对象工厂
   - 延迟创建代理
   - 提供灵活性

### 解决循环依赖的关键

1. **提前暴露**：实例化后立即放入三级缓存
2. **分两步走**：实例化和属性注入分开
3. **延迟决策**：使用工厂延迟创建代理

### 核心流程

```
1. 实例化Bean（new）
2. 放入三级缓存（ObjectFactory）
3. 属性注入（可能触发循环依赖）
4. 从三级缓存获取早期引用
5. 升级到二级缓存
6. 完成后放入一级缓存
```

---

## 🤔 深度思考

### 问题1：为什么单例Bean可以解决循环依赖？

**答案**：因为单例Bean有缓存，可以提前暴露引用

### 问题2：为什么Setter注入可以解决，构造器注入不行？

**答案**：Setter注入可以先创建对象（提前暴露），再注入属性。构造器注入创建对象时就需要依赖，无法提前暴露。

### 问题3：三级缓存中存的是什么？

**答案**：ObjectFactory（lambda表达式），是一个创建早期引用的工厂方法

### 问题4：什么时候从三级缓存升级到二级缓存？

**答案**：当其他Bean需要依赖它时（发生循环依赖时）

### 问题5：为什么需要"正在创建"标记？

**答案**：用于判断是否需要从二三级缓存获取，也用于检测构造器循环依赖

---

## 🎓 学习建议

### 1. 动手调试

在这些地方打断点：
- `getSingleton(String)` 的三个if分支
- `addSingletonFactory` 方法
- `populateBean` 方法

运行循环依赖测试，观察三级缓存的变化。

### 2. 画流程图

自己画一遍A依赖B、B依赖A的完整流程，标注每一步的缓存状态。

### 3. 修改代码实验

- 去掉三级缓存，只用二级，看会发生什么
- 修改为构造器注入，看异常如何抛出
- 打印每一步的缓存状态

### 4. 对比Spring源码

查看Spring的实现：
- `DefaultSingletonBeanRegistry.getSingleton()`
- `AbstractAutowireCapableBeanFactory.doCreateBean()`
- `AbstractAutowireCapableBeanFactory.addSingletonFactory()`

---

希望这份详细的解析能帮你完全理解循环依赖的解决方案！🚀

