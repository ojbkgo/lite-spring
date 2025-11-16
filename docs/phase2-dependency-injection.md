# 第二阶段：依赖注入实现指南

## 🎯 阶段目标

实现Bean之间的依赖关系处理，支持：
- 属性注入（Setter注入）
- 构造器注入
- Bean引用注入
- 简单类型值注入（String、int等）
- 循环依赖的检测和处理（三级缓存）

完成后，你将能够：
```xml
<bean id="userDao" class="com.example.UserDao"/>

<bean id="userService" class="com.example.UserService">
    <!-- 通过setter注入Bean引用 -->
    <property name="userDao" ref="userDao"/>
    <!-- 注入简单值 -->
    <property name="maxRetry" value="3"/>
</bean>

<!-- 构造器注入 -->
<bean id="orderService" class="com.example.OrderService">
    <constructor-arg ref="orderDao"/>
    <constructor-arg value="100"/>
</bean>
```

---

## 📚 理论基础

### 什么是依赖注入（Dependency Injection）？

**传统方式**：对象自己创建依赖
```java
public class UserService {
    private UserDao userDao;
    
    public UserService() {
        this.userDao = new UserDaoImpl();  // 自己创建依赖
    }
}
```

**问题**：
- 紧耦合，难以测试
- 无法替换实现
- 违反开闭原则

**依赖注入方式**：由容器注入依赖
```java
public class UserService {
    private UserDao userDao;
    
    // 通过setter注入
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

**优点**：
- 松耦合
- 易于测试（可以注入Mock对象）
- 灵活替换实现

### 依赖注入的方式

#### 1. Setter注入（属性注入）

**特点**：
- 通过setter方法注入
- 最常用的方式
- 可以选择性注入
- 可以重新注入（修改依赖）

**XML配置**：
```xml
<bean id="userService" class="com.example.UserService">
    <property name="userDao" ref="userDao"/>
    <property name="name" value="UserService"/>
</bean>
```

**Java代码**：
```java
public class UserService {
    private UserDao userDao;
    private String name;
    
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
```

#### 2. 构造器注入

**特点**：
- 通过构造函数注入
- 强制依赖（必须提供）
- 依赖不可变（final字段）
- 更符合不可变对象的设计

**XML配置**：
```xml
<bean id="orderService" class="com.example.OrderService">
    <constructor-arg ref="orderDao"/>
    <constructor-arg value="100"/>
</bean>
```

**Java代码**：
```java
public class OrderService {
    private final OrderDao orderDao;
    private final int maxSize;
    
    public OrderService(OrderDao orderDao, int maxSize) {
        this.orderDao = orderDao;
        this.maxSize = maxSize;
    }
}
```

#### 3. 字段注入（注解方式，第四阶段）

```java
public class UserService {
    @Autowired
    private UserDao userDao;
}
```

### 依赖注入的类型

#### 1. Bean引用注入

注入其他Bean的引用：
```xml
<property name="userDao" ref="userDao"/>
```

#### 2. 简单值注入

注入基本类型和String：
```xml
<property name="name" value="张三"/>
<property name="age" value="18"/>
<property name="salary" value="5000.5"/>
```

#### 3. 集合注入（第二阶段暂不实现）

```xml
<property name="list">
    <list>
        <value>item1</value>
        <value>item2</value>
    </list>
</property>
```

---

## 🔥 循环依赖问题

### 什么是循环依赖？

两个或多个Bean互相依赖：

```java
// A依赖B
public class A {
    private B b;
    public void setB(B b) { this.b = b; }
}

// B依赖A
public class B {
    private A a;
    public void setA(A a) { this.a = a; }
}
```

**问题**：
```
创建A -> 需要B -> 创建B -> 需要A -> 创建A -> 需要B -> ...（无限循环）
```

### Spring的解决方案：三级缓存

Spring使用三级缓存机制解决循环依赖：

```java
// 一级缓存：完全初始化好的Bean
private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

// 二级缓存：提前暴露的Bean（已实例化，未完成属性注入）
private Map<String, Object> earlySingletonObjects = new HashMap<>();

// 三级缓存：Bean工厂
private Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();
```

**解决流程**：
```
1. 创建A的实例 -> 放入三级缓存
2. 为A注入属性，发现需要B
3. 创建B的实例 -> 放入三级缓存
4. 为B注入属性，发现需要A
5. 从三级缓存获取A的早期引用 -> 放入二级缓存
6. B完成属性注入 -> 完成初始化 -> 放入一级缓存
7. A获得B的引用，完成属性注入 -> 完成初始化 -> 放入一级缓存
```

**注意**：
- 只能解决单例Bean的循环依赖
- 只能解决Setter注入的循环依赖
- **构造器注入的循环依赖无法解决**（因为实例还没创建就需要依赖）

---

## 🏗️ 核心组件设计

### 1. PropertyValue - 属性值

**作用**：封装单个属性的名称和值

```java
public class PropertyValue {
    private final String name;      // 属性名
    private final Object value;     // 属性值
    
    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }
    
    // getter方法
}
```

### 2. PropertyValues - 属性值集合

**作用**：存储Bean的所有属性值

```java
public class PropertyValues {
    private final List<PropertyValue> propertyValueList = new ArrayList<>();
    
    public void addPropertyValue(PropertyValue pv) {
        this.propertyValueList.add(pv);
    }
    
    public List<PropertyValue> getPropertyValues() {
        return this.propertyValueList;
    }
}
```

### 3. RuntimeBeanReference - Bean引用

**作用**：表示对另一个Bean的引用

```java
public class RuntimeBeanReference {
    private final String beanName;
    
    public RuntimeBeanReference(String beanName) {
        this.beanName = beanName;
    }
    
    public String getBeanName() {
        return this.beanName;
    }
}
```

**为什么需要这个类？**
- 区分普通字符串值和Bean引用
- `<property name="userDao" ref="userDao"/>` -> RuntimeBeanReference("userDao")
- `<property name="name" value="userDao"/>` -> String("userDao")

### 4. TypedStringValue - 类型化字符串值

**作用**：封装需要类型转换的字符串值

```java
public class TypedStringValue {
    private final String value;
    
    public TypedStringValue(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return this.value;
    }
}
```

**为什么需要？**
- XML中的value都是字符串
- 需要转换为目标类型（int、boolean、double等）

### 5. ConstructorArgument - 构造器参数

**作用**：存储构造器参数列表

```java
public class ConstructorArgument {
    private final List<ValueHolder> argumentValues = new ArrayList<>();
    
    public void addArgumentValue(ValueHolder valueHolder) {
        this.argumentValues.add(valueHolder);
    }
    
    // 内部类：参数值持有者
    public static class ValueHolder {
        private final Object value;
        private final String type;  // 可选：参数类型
        
        public ValueHolder(Object value) {
            this(value, null);
        }
        
        public ValueHolder(Object value, String type) {
            this.value = value;
            this.type = type;
        }
        
        // getter方法
    }
}
```

### 6. BeanDefinition扩展

在第一阶段的BeanDefinition基础上，添加：

```java
public class BeanDefinition {
    // ... 第一阶段的字段 ...
    
    // 新增：属性值集合
    private PropertyValues propertyValues = new PropertyValues();
    
    // 新增：构造器参数
    private ConstructorArgument constructorArgument = new ConstructorArgument();
    
    // 新增：是否有构造器参数
    public boolean hasConstructorArgumentValues() {
        return !this.constructorArgument.isEmpty();
    }
    
    // getter和setter
}
```

### 7. DefaultBeanFactory扩展

需要增强的方法：

```java
public class DefaultBeanFactory extends ... {
    
    // 新增：三级缓存
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();
    
    // 新增：正在创建的Bean集合（检测循环依赖）
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // 增强：createBean方法
    private Object createBean(BeanDefinition bd) {
        // 1. 实例化Bean
        Object bean = instantiateBean(bd);
        
        // 2. 提前暴露Bean（处理循环依赖）
        // 3. 填充属性
        populateBean(bean, bd);
        
        return bean;
    }
    
    // 新增：实例化Bean
    private Object instantiateBean(BeanDefinition bd) {
        // 判断是否有构造器参数
        if (bd.hasConstructorArgumentValues()) {
            return instantiateBeanUsingConstructor(bd);
        } else {
            return instantiateBeanUsingDefaultConstructor(bd);
        }
    }
    
    // 新增：填充属性
    private void populateBean(Object bean, BeanDefinition bd) {
        // 遍历属性值
        for (PropertyValue pv : bd.getPropertyValues().getPropertyValues()) {
            // 解析属性值
            // 通过反射设置属性
        }
    }
}
```

---

## 📋 实现步骤

### 步骤1：创建基础数据结构

**任务**：
1. 创建 `PropertyValue` 类
2. 创建 `PropertyValues` 类
3. 创建 `RuntimeBeanReference` 类
4. 创建 `TypedStringValue` 类
5. 创建 `ConstructorArgument` 类

**目的**：
- 封装属性信息
- 区分Bean引用和普通值

**测试思路**：
```java
@Test
public void testPropertyValue() {
    PropertyValue pv = new PropertyValue("name", "Tom");
    assertEquals("name", pv.getName());
    assertEquals("Tom", pv.getValue());
}

@Test
public void testRuntimeBeanReference() {
    RuntimeBeanReference ref = new RuntimeBeanReference("userDao");
    assertEquals("userDao", ref.getBeanName());
}
```

---

### 步骤2：扩展BeanDefinition

**任务**：
- 在BeanDefinition中添加 `PropertyValues` 字段
- 在BeanDefinition中添加 `ConstructorArgument` 字段
- 添加相关的getter/setter方法

**关键代码**：
```java
public class BeanDefinition {
    // ... 已有字段 ...
    
    private PropertyValues propertyValues = new PropertyValues();
    private ConstructorArgument constructorArgument = new ConstructorArgument();
    
    public PropertyValues getPropertyValues() {
        return this.propertyValues;
    }
    
    public ConstructorArgument getConstructorArgument() {
        return this.constructorArgument;
    }
    
    public boolean hasConstructorArgumentValues() {
        return !this.constructorArgument.getArgumentValues().isEmpty();
    }
}
```

**测试思路**：
```java
@Test
public void testBeanDefinitionWithProperty() {
    BeanDefinition bd = new BeanDefinition("com.example.UserService");
    PropertyValue pv = new PropertyValue("userDao", new RuntimeBeanReference("userDao"));
    bd.getPropertyValues().addPropertyValue(pv);
    
    assertEquals(1, bd.getPropertyValues().getPropertyValues().size());
}
```

---

### 步骤3：增强XmlBeanDefinitionReader

**任务**：解析XML中的 `<property>` 和 `<constructor-arg>` 元素

**XML示例**：
```xml
<bean id="userService" class="com.example.UserService">
    <!-- ref属性表示Bean引用 -->
    <property name="userDao" ref="userDao"/>
    
    <!-- value属性表示简单值 -->
    <property name="maxRetry" value="3"/>
</bean>

<bean id="orderService" class="com.example.OrderService">
    <constructor-arg ref="orderDao"/>
    <constructor-arg value="100"/>
</bean>
```

**解析逻辑**：
```
1. 在parseBeanDefinition方法中
2. 获取所有<property>子元素
3. 遍历每个<property>
   - 获取name属性
   - 检查是ref还是value
   - 如果是ref -> 创建RuntimeBeanReference
   - 如果是value -> 创建TypedStringValue
   - 创建PropertyValue并添加到BeanDefinition

4. 获取所有<constructor-arg>子元素
5. 遍历每个<constructor-arg>
   - 检查是ref还是value
   - 创建ValueHolder
   - 添加到ConstructorArgument
```

**关键代码思路**：
```java
private void parseBeanDefinition(Element element) {
    // ... 已有的id、class等解析 ...
    
    // 解析property元素
    parsePropertyElements(element, bd);
    
    // 解析constructor-arg元素
    parseConstructorArgElements(element, bd);
}

private void parsePropertyElements(Element beanElement, BeanDefinition bd) {
    NodeList propertyNodes = beanElement.getElementsByTagName("property");
    for (int i = 0; i < propertyNodes.getLength(); i++) {
        Element propertyElement = (Element) propertyNodes.item(i);
        
        String name = propertyElement.getAttribute("name");
        String ref = propertyElement.getAttribute("ref");
        String value = propertyElement.getAttribute("value");
        
        Object val;
        if (ref != null && !ref.isEmpty()) {
            val = new RuntimeBeanReference(ref);
        } else if (value != null && !value.isEmpty()) {
            val = new TypedStringValue(value);
        } else {
            throw new BeansException("property必须指定ref或value");
        }
        
        PropertyValue pv = new PropertyValue(name, val);
        bd.getPropertyValues().addPropertyValue(pv);
    }
}
```

**测试思路**：
```java
@Test
public void testParsePropertyElements() {
    // 准备XML配置
    // 解析
    BeanDefinition bd = factory.getBeanDefinition("userService");
    
    // 验证属性被解析
    PropertyValues pvs = bd.getPropertyValues();
    assertEquals(2, pvs.getPropertyValues().size());
    
    // 验证Bean引用
    PropertyValue pv1 = pvs.getPropertyValues().get(0);
    assertTrue(pv1.getValue() instanceof RuntimeBeanReference);
}
```

---

### 步骤4：实现属性注入（核心）

**任务**：在DefaultBeanFactory中实现populateBean方法

**挑战**：
1. 如何通过反射调用setter方法？
2. 如何解析Bean引用？
3. 如何进行类型转换？

**实现思路**：

```
populateBean(bean, bd) {
    1. 获取BeanDefinition的PropertyValues
    2. 遍历每个PropertyValue
    3. 解析属性值：
       - 如果是RuntimeBeanReference -> 调用getBean获取引用的Bean
       - 如果是TypedStringValue -> 转换为目标类型
    4. 通过反射调用setter方法设置属性
}
```

**关键技术点**：

**1. 查找setter方法**：
```java
// 属性名：userDao
// setter方法名：setUserDao

String methodName = "set" + 首字母大写(propertyName);
Method method = bean.getClass().getMethod(methodName, 参数类型);
method.invoke(bean, 参数值);
```

**2. 解析属性值**：
```java
private Object resolveValueIfNecessary(Object value) {
    if (value instanceof RuntimeBeanReference) {
        RuntimeBeanReference ref = (RuntimeBeanReference) value;
        return getBean(ref.getBeanName());  // 递归获取Bean
    } else if (value instanceof TypedStringValue) {
        return ((TypedStringValue) value).getValue();
    }
    return value;
}
```

**3. 类型转换**：
```java
private Object convertValue(String value, Class<?> targetType) {
    if (targetType == String.class) {
        return value;
    } else if (targetType == int.class || targetType == Integer.class) {
        return Integer.parseInt(value);
    } else if (targetType == boolean.class || targetType == Boolean.class) {
        return Boolean.parseBoolean(value);
    } else if (targetType == double.class || targetType == Double.class) {
        return Double.parseDouble(value);
    }
    // ... 其他类型
    return value;
}
```

**测试思路**：
```java
@Test
public void testPropertyInjection() {
    // 配置两个Bean，一个依赖另一个
    BeanDefinition daoBd = new BeanDefinition("com.example.UserDao");
    factory.registerBeanDefinition("userDao", daoBd);
    
    BeanDefinition serviceBd = new BeanDefinition("com.example.UserService");
    serviceBd.getPropertyValues().addPropertyValue(
        new PropertyValue("userDao", new RuntimeBeanReference("userDao"))
    );
    factory.registerBeanDefinition("userService", serviceBd);
    
    // 获取Bean
    UserService service = (UserService) factory.getBean("userService");
    
    // 验证依赖已注入
    assertNotNull(service.getUserDao());
}
```

---

### 步骤5：实现构造器注入

**任务**：根据构造器参数创建Bean实例

**挑战**：
1. 如何选择合适的构造器？
2. 如何解析构造器参数？
3. 参数类型匹配问题

**实现思路**：

```
instantiateBeanUsingConstructor(bd) {
    1. 获取ConstructorArgument
    2. 解析所有参数值（Bean引用、简单值）
    3. 获取参数类型数组
    4. 查找匹配的构造器
    5. 调用构造器创建实例
}
```

**简化方案（第二阶段）**：
- 只支持按参数顺序匹配
- 不支持按参数类型自动匹配
- 参数类型通过实际值推断

**关键代码思路**：
```java
private Object instantiateBeanUsingConstructor(BeanDefinition bd) {
    // 1. 获取构造器参数
    ConstructorArgument args = bd.getConstructorArgument();
    List<Object> resolvedArgs = new ArrayList<>();
    
    // 2. 解析参数值
    for (ValueHolder holder : args.getArgumentValues()) {
        Object resolvedValue = resolveValueIfNecessary(holder.getValue());
        resolvedArgs.add(resolvedValue);
    }
    
    // 3. 获取参数类型
    Class<?>[] paramTypes = new Class<?>[resolvedArgs.size()];
    for (int i = 0; i < resolvedArgs.size(); i++) {
        paramTypes[i] = resolvedArgs.get(i).getClass();
    }
    
    // 4. 查找构造器并创建实例
    Class<?> clazz = Class.forName(bd.getBeanClassName());
    Constructor<?> constructor = clazz.getConstructor(paramTypes);
    return constructor.newInstance(resolvedArgs.toArray());
}
```

**测试思路**：
```java
@Test
public void testConstructorInjection() {
    BeanDefinition daoBd = new BeanDefinition("com.example.OrderDao");
    factory.registerBeanDefinition("orderDao", daoBd);
    
    BeanDefinition serviceBd = new BeanDefinition("com.example.OrderService");
    serviceBd.getConstructorArgument().addArgumentValue(
        new ValueHolder(new RuntimeBeanReference("orderDao"))
    );
    serviceBd.getConstructorArgument().addArgumentValue(
        new ValueHolder(new TypedStringValue("100"))
    );
    factory.registerBeanDefinition("orderService", serviceBd);
    
    OrderService service = (OrderService) factory.getBean("orderService");
    assertNotNull(service);
}
```

---

### 步骤6：处理循环依赖（核心难点）

**任务**：实现三级缓存机制

**第一步：添加三级缓存**

```java
public class DefaultBeanFactory {
    // 一级缓存：完全初始化的单例Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    // 二级缓存：早期暴露的Bean（已实例化，未属性注入）
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    
    // 三级缓存：Bean工厂
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();
    
    // 正在创建的Bean（检测循环依赖）
    private final Set<String> singletonsCurrentlyInCreation = 
        Collections.newSetFromMap(new ConcurrentHashMap<>());
}
```

**第二步：改造getBean方法**

```
getBean(beanName) {
    // 1. 尝试从缓存获取（三级缓存依次查找）
    Object bean = getSingleton(beanName);
    if (bean != null) {
        return bean;
    }
    
    // 2. 标记为正在创建
    markBeanAsCurrentlyInCreation(beanName);
    
    // 3. 创建Bean
    bean = createBean(bd);
    
    // 4. 从正在创建集合中移除
    removeFromCurrentlyInCreation(beanName);
    
    return bean;
}
```

**第三步：getSingleton方法（三级缓存查找）**

```java
private Object getSingleton(String beanName) {
    // 1. 从一级缓存获取
    Object bean = singletonObjects.get(beanName);
    if (bean != null) {
        return bean;
    }
    
    // 2. 如果正在创建，从二级缓存获取
    if (isSingletonCurrentlyInCreation(beanName)) {
        bean = earlySingletonObjects.get(beanName);
        if (bean != null) {
            return bean;
        }
        
        // 3. 从三级缓存获取
        ObjectFactory<?> factory = singletonFactories.get(beanName);
        if (factory != null) {
            bean = factory.getObject();
            // 升级到二级缓存
            earlySingletonObjects.put(beanName, bean);
            singletonFactories.remove(beanName);
        }
    }
    
    return bean;
}
```

**第四步：createBean中提前暴露Bean**

```java
private Object createBean(BeanDefinition bd) {
    // 1. 实例化
    Object bean = instantiateBean(bd);
    
    // 2. 单例Bean提前暴露（放入三级缓存）
    if (bd.isSingleton()) {
        addSingletonFactory(beanName, () -> bean);
    }
    
    // 3. 属性注入（可能触发循环依赖）
    populateBean(bean, bd);
    
    // 4. 初始化完成，放入一级缓存
    if (bd.isSingleton()) {
        singletonObjects.put(beanName, bean);
        earlySingletonObjects.remove(beanName);
        singletonFactories.remove(beanName);
    }
    
    return bean;
}
```

**测试思路**：
```java
@Test
public void testCircularDependency() {
    // A依赖B，B依赖A
    BeanDefinition aBd = new BeanDefinition("com.example.A");
    aBd.getPropertyValues().addPropertyValue(
        new PropertyValue("b", new RuntimeBeanReference("b"))
    );
    factory.registerBeanDefinition("a", aBd);
    
    BeanDefinition bBd = new BeanDefinition("com.example.B");
    bBd.getPropertyValues().addPropertyValue(
        new PropertyValue("a", new RuntimeBeanReference("a"))
    );
    factory.registerBeanDefinition("b", bBd);
    
    // 应该能正确解决循环依赖
    A a = (A) factory.getBean("a");
    B b = (B) factory.getBean("b");
    
    assertNotNull(a.getB());
    assertNotNull(b.getA());
    assertSame(a, b.getA());
    assertSame(b, a.getB());
}

@Test
public void testConstructorCircularDependency() {
    // 构造器循环依赖应该抛出异常
    BeanDefinition aBd = new BeanDefinition("com.example.A");
    aBd.getConstructorArgument().addArgumentValue(
        new ValueHolder(new RuntimeBeanReference("b"))
    );
    
    BeanDefinition bBd = new BeanDefinition("com.example.B");
    bBd.getConstructorArgument().addArgumentValue(
        new ValueHolder(new RuntimeBeanReference("a"))
    );
    
    factory.registerBeanDefinition("a", aBd);
    factory.registerBeanDefinition("b", bBd);
    
    // 应该抛出异常
    assertThrows(BeansException.class, () -> {
        factory.getBean("a");
    });
}
```

---

## 🎯 关键难点解析

### 难点1：属性注入的反射实现

**问题**：如何通过属性名找到setter方法？

**解决方案**：
```java
// 属性名：userDao
// 方法名：setUserDao
String setterName = "set" + propertyName.substring(0, 1).toUpperCase() 
                    + propertyName.substring(1);

// 问题：如何获取参数类型？
// 方案1：通过属性值的类型推断
Class<?> paramType = value.getClass();
Method method = bean.getClass().getMethod(setterName, paramType);

// 方案2：使用Java内省API（推荐）
import java.beans.PropertyDescriptor;

PropertyDescriptor pd = new PropertyDescriptor(propertyName, bean.getClass());
Method writeMethod = pd.getWriteMethod();  // 获取setter方法
writeMethod.invoke(bean, value);
```

### 难点2：类型转换

**问题**：XML中的value都是String，如何转换为目标类型？

**解决方案**：
```java
// 简化版本：支持常用类型
private Object convertValue(String strValue, Class<?> targetType) {
    if (targetType == String.class) {
        return strValue;
    }
    if (targetType == int.class || targetType == Integer.class) {
        return Integer.parseInt(strValue);
    }
    if (targetType == long.class || targetType == Long.class) {
        return Long.parseLong(strValue);
    }
    if (targetType == double.class || targetType == Double.class) {
        return Double.parseDouble(strValue);
    }
    if (targetType == boolean.class || targetType == Boolean.class) {
        return Boolean.parseBoolean(strValue);
    }
    throw new BeansException("不支持的类型转换: " + targetType);
}
```

### 难点3：构造器参数匹配

**问题**：如何找到匹配的构造器？

**简化方案**：
```java
// 第二阶段：简单方案
// 根据参数个数和类型精确匹配
Class<?>[] paramTypes = getParameterTypes(args);
Constructor<?> ctor = clazz.getConstructor(paramTypes);
```

**完整方案**（第三阶段优化）：
- 支持类型自动转换
- 支持父类/接口匹配
- 支持参数名匹配

### 难点4：循环依赖检测

**问题**：如何检测循环依赖？

**方案**：使用 `singletonsCurrentlyInCreation` 集合
```java
if (singletonsCurrentlyInCreation.contains(beanName)) {
    throw new BeansException("检测到循环依赖: " + beanName);
}
```

**注意**：
- Setter注入的循环依赖可以通过三级缓存解决，不应抛异常
- 构造器注入的循环依赖无法解决，必须抛异常

---

## 📝 XML配置示例

### 完整的配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <!-- DAO层 -->
    <bean id="userDao" class="com.litespring.demo.dao.UserDaoImpl"/>
    
    <bean id="orderDao" class="com.litespring.demo.dao.OrderDaoImpl"/>
    
    <!-- Service层：Setter注入 -->
    <bean id="userService" class="com.litespring.demo.service.UserServiceImpl">
        <!-- 注入Bean引用 -->
        <property name="userDao" ref="userDao"/>
        <!-- 注入简单值 -->
        <property name="maxRetry" value="3"/>
        <property name="timeout" value="5000"/>
    </bean>
    
    <!-- Service层：构造器注入 -->
    <bean id="orderService" class="com.litespring.demo.service.OrderServiceImpl">
        <constructor-arg ref="orderDao"/>
        <constructor-arg value="100"/>
    </bean>
    
    <!-- 循环依赖示例（Setter注入可以解决） -->
    <bean id="a" class="com.litespring.test.v2.A">
        <property name="b" ref="b"/>
    </bean>
    
    <bean id="b" class="com.litespring.test.v2.B">
        <property name="a" ref="a"/>
    </bean>
</beans>
```

---

## 🎓 学习建议

### 实现顺序建议

1. **先实现简单的Setter注入**（不考虑循环依赖）
2. **再实现构造器注入**
3. **最后实现循环依赖处理**

**原因**：
- 循环依赖是最复杂的部分
- 先把基础功能跑通
- 再逐步增强

### 调试技巧

1. **打印日志**
```java
System.out.println("正在创建Bean: " + beanName);
System.out.println("注入属性: " + propertyName + " = " + value);
```

2. **使用断点**
- 在 `populateBean()` 方法打断点
- 在 `getBean()` 方法打断点
- 观察Bean的创建和注入过程

3. **测试驱动**
- 先写简单的测试
- 逐步增加复杂度
- 每个功能点都有测试覆盖

---

## 🤔 思考题

实现前先思考这些问题：

1. **为什么需要RuntimeBeanReference类？**
   - 不能直接用String表示Bean引用吗？

2. **三级缓存的每一级分别存储什么？**
   - 为什么需要三级？两级不行吗？

3. **为什么构造器循环依赖无法解决？**
   - Setter注入可以解决的原理是什么？

4. **原型Bean能解决循环依赖吗？**
   - 为什么只有单例Bean可以？

5. **类型转换有哪些边界情况需要处理？**
   - null值怎么办？
   - 转换失败怎么办？

---

## 📊 与第一阶段的对比

| 方面 | 第一阶段 | 第二阶段 |
|------|---------|---------|
| **Bean创建** | ✅ 无参构造 | ✅ 有参构造 |
| **依赖关系** | ❌ 不支持 | ✅ 支持 |
| **属性注入** | ❌ 不支持 | ✅ Setter注入 |
| **构造器注入** | ❌ 不支持 | ✅ 支持 |
| **循环依赖** | ❌ 未处理 | ✅ 三级缓存 |
| **XML配置** | ✅ 基本属性 | ✅ property和constructor-arg |

---

## ✅ 完成标志

完成第二阶段后，你应该能够：

1. ✅ 通过XML配置Bean之间的依赖关系
2. ✅ 使用Setter方式注入Bean引用
3. ✅ 使用Setter方式注入简单值
4. ✅ 使用构造器注入
5. ✅ 自动解决Setter注入的循环依赖
6. ✅ 检测构造器注入的循环依赖
7. ✅ 支持基本类型的自动转换

---

## 🚀 准备好了吗？

现在你可以：

1. **仔细阅读这份文档**（30-60分钟）
2. **思考上面的思考题**
3. **理解三级缓存的原理**
4. **准备好后告诉我**

我会为你提供：
- 完整的测试用例
- 关键类的实现
- 测试用的Bean类
- XML配置文件

第二阶段是整个框架最核心、最复杂的部分，理解好这部分原理非常重要！

慢慢消化，有任何疑问随时问我！💪

