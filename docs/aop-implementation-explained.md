# AOP实现逻辑详解

本文档详细解析lite-spring框架第五阶段AOP的实现原理，包括JDK动态代理、拦截器链、自动代理创建等核心机制。

---

## 📚 目录

1. [核心概念总览](#1-核心概念总览)
2. [类的职责和关系](#2-类的职责和关系)
3. [JDK动态代理原理](#3-jdk动态代理原理)
4. [拦截器链执行机制](#4-拦截器链执行机制)
5. [自动代理创建流程](#5-自动代理创建流程)
6. [完整执行示例](#6-完整执行示例)
7. [与IoC容器的集成](#7-与ioc容器的集成)

---

## 1. 核心概念总览

### AOP是什么？

**AOP（Aspect-Oriented Programming）**：面向切面编程

**核心思想**：将横切关注点（日志、事务、安全等）从业务逻辑中分离出来

### 一个直观的例子

**没有AOP（横切逻辑混在业务中）**：
```java
public class UserService {
    public void saveUser(String name) {
        // 1. 日志
        System.out.println("开始保存用户");
        
        // 2. 权限检查
        checkPermission();
        
        // 3. 性能监控
        long start = System.currentTimeMillis();
        
        // 4. 事务
        beginTransaction();
        
        try {
            // 5. 业务逻辑（被淹没了！）
            userDao.save(name);
            
            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
        
        // 6. 性能监控
        long end = System.currentTimeMillis();
        System.out.println("耗时: " + (end - start));
        
        // 7. 日志
        System.out.println("保存完成");
    }
}
```

**使用AOP（业务逻辑清晰）**：
```java
// 业务代码：干净！
public class UserService {
    public void saveUser(String name) {
        userDao.save(name);  // 只有核心业务逻辑
    }
}

// 日志切面
public class LoggingAspect implements MethodInterceptor {
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println("开始执行: " + invocation.getMethod().getName());
        Object result = invocation.proceed();
        System.out.println("执行完成");
        return result;
    }
}
```

### AOP的5个核心概念

| 概念 | 说明 | 在代码中的体现 |
|------|------|---------------|
| **JoinPoint（连接点）** | 程序执行中的某个点 | Method（方法） |
| **Pointcut（切点）** | 匹配连接点的规则 | `Pointcut.matches(method, class)` |
| **Advice（通知）** | 在切点执行的动作 | `MethodBeforeAdvice`等 |
| **Advisor（通知器）** | Pointcut + Advice | `DefaultPointcutAdvisor` |
| **Proxy（代理）** | 包装目标对象 | JDK Proxy对象 |

---

## 2. 类的职责和关系

### 2.1 接口层（定义规范）

#### Advice - 通知标记接口

```java
public interface Advice {
    // 标记接口，无方法
    // 所有通知类型的父接口
}
```

**作用**：统一所有通知类型的标记

#### MethodBeforeAdvice - 前置通知

```java
public interface MethodBeforeAdvice extends Advice {
    void before(Method method, Object[] args, Object target) throws Throwable;
}
```

**何时执行**：在目标方法**执行前**

**能做什么**：
- 记录日志
- 参数校验
- 权限检查
- **不能阻止方法执行**
- **不能修改返回值**

#### AfterReturningAdvice - 返回后通知

```java
public interface AfterReturningAdvice extends Advice {
    void afterReturning(Object returnValue, Method method, Object[] args, Object target);
}
```

**何时执行**：在目标方法**成功返回后**

**能做什么**：
- 记录返回值
- 后置处理
- **不能修改返回值**（已经返回了）
- **异常时不执行**

#### MethodInterceptor - 方法拦截器（环绕通知）

```java
public interface MethodInterceptor extends Advice {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

**何时执行**：**完全控制**方法执行

**能做什么**（最强大）：
- ✅ 在方法前后执行逻辑
- ✅ 决定是否调用目标方法
- ✅ 修改参数
- ✅ 修改返回值
- ✅ 处理异常
- ✅ 一切皆可控制！

**示例**：
```java
public Object invoke(MethodInvocation invocation) throws Throwable {
    // 方法前
    System.out.println("Before");
    
    // 决定是否执行
    if (shouldProceed()) {
        Object result = invocation.proceed();  // 执行
        
        // 方法后
        System.out.println("After");
        
        // 可以修改返回值
        return modifyResult(result);
    } else {
        return null;  // 不执行，直接返回
    }
}
```

#### Pointcut - 切点

```java
public interface Pointcut {
    boolean matches(Method method, Class<?> targetClass);
}
```

**作用**：判断方法是否需要被拦截

**实现**：
```java
public class NameMatchPointcut implements Pointcut {
    private Set<String> methodNames;
    
    public boolean matches(Method method, Class<?> targetClass) {
        return methodNames.contains(method.getName());
    }
}
```

#### Advisor - 通知器

```java
public interface Advisor {
    Advice getAdvice();
}

public interface PointcutAdvisor extends Advisor {
    Pointcut getPointcut();
}
```

**作用**：组合Pointcut和Advice

**关系**：
```
Advisor {
    Pointcut pointcut;  // 在哪里拦截
    Advice advice;      // 做什么
}
```

---

### 2.2 实现层（具体实现）

#### ReflectiveMethodInvocation - 拦截器链执行器 ⭐核心

**职责**：
- 持有拦截器链
- 按顺序执行每个拦截器
- 最后执行目标方法

**核心字段**：
```java
public class ReflectiveMethodInvocation {
    private Object target;              // 目标对象
    private Method method;              // 目标方法
    private Object[] arguments;         // 方法参数
    private List<Object> interceptors;  // 拦截器链
    private int currentInterceptorIndex = -1;  // 当前索引（关键！）
}
```

**核心方法**：`proceed()`

```java
public Object proceed() throws Throwable {
    // 【终止条件】索引到达最后一个
    if (currentInterceptorIndex == interceptors.size() - 1) {
        return invokeJoinpoint();  // 调用目标方法
    }
    
    // 【获取下一个】索引+1，获取拦截器
    Object interceptor = interceptors.get(++currentInterceptorIndex);
    
    // 【执行拦截器】根据类型分别处理
    if (interceptor instanceof MethodInterceptor) {
        // 环绕通知：拦截器内部会调用proceed()
        return ((MethodInterceptor) interceptor).invoke(this);
        
    } else if (interceptor instanceof MethodBeforeAdvice) {
        // 前置通知：先执行通知，再继续链
        ((MethodBeforeAdvice) interceptor).before(method, args, target);
        return proceed();  // 继续
        
    } else if (interceptor instanceof AfterReturningAdvice) {
        // 返回后通知：先执行方法，再执行通知
        Object result = proceed();  // 先执行
        ((AfterReturningAdvice) interceptor).afterReturning(result, ...);
        return result;
    }
}
```

**关键理解**：
- `currentInterceptorIndex` 像一个指针，指向当前要执行的拦截器
- 每次调用 `proceed()` 都会执行下一个拦截器
- 最后执行目标方法
- **这是递归调用！**

#### JdkDynamicAopProxy - JDK代理实现 ⭐核心

**职责**：
- 创建JDK动态代理对象
- 拦截所有方法调用
- 获取拦截器链并执行

**两个角色**：
1. **AopProxy**：创建代理的工厂
2. **InvocationHandler**：拦截方法调用的处理器

**创建代理**：
```java
public Object getProxy() {
    Class<?>[] interfaces = advised.getTarget().getClass().getInterfaces();
    
    return Proxy.newProxyInstance(
        classLoader,
        interfaces,  // 目标对象实现的接口
        this         // 自己作为InvocationHandler
    );
}
```

**拦截调用**：
```java
public Object invoke(Object proxy, Method method, Object[] args) {
    Object target = advised.getTarget();
    
    // 1. 获取拦截器链（根据方法匹配Advisor）
    List<Object> chain = advised.getInterceptors(method);
    
    // 2. 没有拦截器，直接调用
    if (chain.isEmpty()) {
        return method.invoke(target, args);
    }
    
    // 3. 有拦截器，执行拦截器链
    MethodInvocation invocation = new ReflectiveMethodInvocation(
        target, method, args, chain
    );
    return invocation.proceed();
}
```

#### ProxyFactory - 代理工厂

**职责**：
- 简化代理创建
- 封装配置
- 选择代理策略（JDK vs CGLIB）

**继承关系**：
```java
AdvisedSupport  // 持有配置
    ├── target（目标对象）
    ├── advisors[]（通知器列表）
    └── getInterceptors(method)
    
ProxyFactory extends AdvisedSupport  // 添加创建代理的方法
    └── getProxy()
```

**使用流程**：
```java
// 1. 创建工厂
ProxyFactory factory = new ProxyFactory();

// 2. 设置目标
factory.setTarget(target);

// 3. 添加Advisor
factory.addAdvisor(advisor1);
factory.addAdvisor(advisor2);

// 4. 创建代理
Object proxy = factory.getProxy();
```

---

### 2.3 集成层（与IoC集成）

#### DefaultAdvisorAutoProxyCreator - 自动代理创建器 ⭐核心

**职责**：
- 作为BeanPostProcessor
- 自动为Bean创建代理
- 查找匹配的Advisor
- 集成AOP到IoC容器

**关键方法**：
```java
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // 1. 跳过AOP基础类（Advice、Advisor等）
    if (isInfrastructureClass(bean.getClass())) {
        return bean;
    }
    
    // 2. 查找匹配的Advisor
    List<Advisor> advisors = getMatchingAdvisors(bean.getClass());
    
    // 3. 如果有匹配的，创建代理
    if (!advisors.isEmpty()) {
        return createProxy(bean, advisors);
    }
    
    // 4. 没有匹配的，返回原Bean
    return bean;
}
```

**工作流程**：
```
Bean初始化完成
  ↓
postProcessAfterInitialization(bean)
  ↓
是AOP基础类？
  YES → 返回原Bean
  NO  ↓
查找容器中所有Advisor
  ↓
检查Advisor是否匹配当前Bean
  ↓
有匹配的Advisor？
  YES → 创建代理 → 返回代理对象
  NO  → 返回原Bean
```

---

## 3. JDK动态代理原理

### 3.1 静态代理 vs 动态代理

#### 静态代理（传统方式）

```java
// 接口
public interface UserService {
    void saveUser(String name);
}

// 实现
public class UserServiceImpl implements UserService {
    public void saveUser(String name) {
        System.out.println("保存: " + name);
    }
}

// 代理类（手动编写）
public class UserServiceProxy implements UserService {
    private UserService target;
    
    public UserServiceProxy(UserService target) {
        this.target = target;
    }
    
    public void saveUser(String name) {
        System.out.println("Before");  // 增强逻辑
        target.saveUser(name);         // 调用目标
        System.out.println("After");   // 增强逻辑
    }
}

// 使用
UserService target = new UserServiceImpl();
UserService proxy = new UserServiceProxy(target);
proxy.saveUser("Tom");
```

**问题**：
- ❌ 每个类都要写代理类
- ❌ 代码重复
- ❌ 不灵活

#### 动态代理（JDK方式）

```java
// 目标对象
UserService target = new UserServiceImpl();

// 创建代理（运行时动态生成代理类）
UserService proxy = (UserService) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    new InvocationHandler() {
        public Object invoke(Object proxy, Method method, Object[] args) {
            System.out.println("Before");
            Object result = method.invoke(target, args);
            System.out.println("After");
            return result;
        }
    }
);

// 使用
proxy.saveUser("Tom");
```

**优势**：
- ✅ 运行时生成，不需要手写代理类
- ✅ 一个InvocationHandler处理所有方法
- ✅ 灵活、可复用

### 3.2 JDK代理的工作原理

**Proxy.newProxyInstance做了什么？**

```
1. 根据接口定义，动态生成代理类的字节码
   class $Proxy0 implements UserService {
       private InvocationHandler h;
       
       public void saveUser(String name) {
           // 调用InvocationHandler
           h.invoke(this, saveUserMethod, new Object[]{name});
       }
   }

2. 加载代理类
   Class<?> proxyClass = defineClass(字节码);

3. 创建代理实例
   return proxyClass.newInstance(invocationHandler);
```

**调用流程**：
```
proxy.saveUser("Tom")  ← 用户调用
  ↓
$Proxy0.saveUser("Tom")  ← 代理类（JVM生成）
  ↓
InvocationHandler.invoke(proxy, saveUserMethod, ["Tom"])  ← 拦截
  ↓
// 在invoke中可以添加额外逻辑
method.invoke(target, args)  ← 调用真实对象
  ↓
target.saveUser("Tom")  ← 目标方法执行
```

### 3.3 lite-spring的JdkDynamicAopProxy实现

**完整代码解析**：

```java
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private final AdvisedSupport advised;  // 【持有配置】
    
    public JdkDynamicAopProxy(AdvisedSupport config) {
        this.advised = config;
        // advised包含：
        // - target: 目标对象
        // - advisors: 所有通知器
    }
    
    // 【创建代理对象】
    @Override
    public Object getProxy() {
        // 获取目标对象的接口
        Class<?>[] interfaces = advised.getTarget().getClass().getInterfaces();
        
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("目标对象没有实现接口");
        }
        
        // 创建代理
        return Proxy.newProxyInstance(
            getClass().getClassLoader(),
            interfaces,
            this  // 自己作为InvocationHandler
        );
    }
    
    // 【拦截所有方法调用】
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) 
            throws Throwable {
        
        Object target = advised.getTarget();
        
        // 【1】特殊处理：Object类的方法直接调用
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
            // equals、hashCode、toString等
        }
        
        // 【2】获取匹配的拦截器链
        List<Object> chain = advised.getInterceptors(method);
        // 遍历所有Advisor，找出切点匹配当前方法的
        
        // 【3】没有拦截器：直接调用目标方法
        if (chain.isEmpty()) {
            return method.invoke(target, args);
        }
        
        // 【4】有拦截器：执行拦截器链
        MethodInvocation invocation = new ReflectiveMethodInvocation(
            target, method, args, chain
        );
        
        return invocation.proceed();
    }
}
```

**关键点**：
- `advised.getInterceptors(method)` 获取匹配的拦截器
- 没有拦截器就直接调用，有拦截器就执行链
- Object类的方法不拦截（equals等）

---

## 4. 拦截器链执行机制

### 4.1 什么是拦截器链？

**场景**：一个方法匹配了3个Advisor

```java
// 定义了3个Advisor
Advisor1: Pointcut(matches "saveUser") + BeforeAdvice
Advisor2: Pointcut(matches "saveUser") + AroundAdvice  
Advisor3: Pointcut(matches "saveUser") + AfterAdvice

// 调用proxy.saveUser("Tom")时
拦截器链 = [BeforeAdvice, AroundAdvice, AfterAdvice]
```

### 4.2 责任链模式

**设计模式**：每个拦截器处理后，决定是否传递给下一个

```
拦截器1 → 拦截器2 → 拦截器3 → 目标方法
```

**核心**：通过递归调用 `proceed()` 实现

### 4.3 执行流程详解

**假设**：
- 拦截器链：`[BeforeAdvice, AroundAdvice]`
- 方法：`saveUser("Tom")`

**执行步骤**：

```java
// 【第1次调用proceed】
proceed() {
    currentIndex = -1
    currentIndex == size - 1?  // -1 == 1? NO
    
    interceptor = interceptors[++currentIndex]  // index=0
    interceptor = BeforeAdvice
    
    if (interceptor instanceof MethodBeforeAdvice) {
        beforeAdvice.before(method, args, target)
        // 输出: "【Before】调用方法: saveUser"
        
        return proceed()  // 【递归】第2次调用
    }
}

// 【第2次调用proceed】
proceed() {
    currentIndex = 0
    currentIndex == size - 1?  // 0 == 1? NO
    
    interceptor = interceptors[++currentIndex]  // index=1
    interceptor = AroundAdvice
    
    if (interceptor instanceof MethodInterceptor) {
        return aroundAdvice.invoke(this)
        // 进入Around通知内部
    }
}

// 【AroundAdvice内部】
aroundAdvice.invoke(invocation) {
    System.out.println("【Around】方法开始")
    
    result = invocation.proceed()  // 【递归】第3次调用
    
    System.out.println("【Around】方法结束")
    return result
}

// 【第3次调用proceed】
proceed() {
    currentIndex = 1
    currentIndex == size - 1?  // 1 == 1? YES! 到达终点
    
    return invokeJoinpoint()  // 调用目标方法
}

invokeJoinpoint() {
    return method.invoke(target, args)
    // target.saveUser("Tom")
    // 输出: "【目标】保存用户: Tom"
}

// 【返回】
// 第3次proceed返回 → Around通知继续 → 第2次proceed返回 → 第1次proceed返回
```

**输出顺序**：
```
【Before】调用方法: saveUser
【Around】方法开始
【目标】保存用户: Tom
【Around】方法结束
```

### 4.4 三种通知类型的执行差异

#### Before通知（前置）
```java
if (interceptor instanceof MethodBeforeAdvice) {
    advice.before(method, args, target);  // 先执行通知
    return proceed();  // 再继续链
}
```

**流程**：
```
执行before → 调用proceed → 后续拦截器/目标方法
```

#### AfterReturning通知（返回后）
```java
if (interceptor instanceof AfterReturningAdvice) {
    Object result = proceed();  // 先执行后续
    advice.afterReturning(result, ...);  // 再执行通知
    return result;
}
```

**流程**：
```
调用proceed → 后续拦截器/目标方法 → 执行afterReturning
```

#### Around通知（环绕）
```java
if (interceptor instanceof MethodInterceptor) {
    return interceptor.invoke(this);
    // 拦截器内部控制何时调用proceed
}

// Around通知内部
public Object invoke(MethodInvocation invocation) {
    // 前置逻辑
    System.out.println("Before");
    
    // 执行目标方法
    Object result = invocation.proceed();
    
    // 后置逻辑
    System.out.println("After");
    
    return result;
}
```

**流程**：
```
Around前置逻辑 → 调用proceed → 后续/目标方法 → Around后置逻辑
```

---

## 5. 自动代理创建流程

### 5.1 问题：如何让代理对用户透明？

**希望的使用方式**：
```java
// 用户不需要手动创建代理
UserService service = ctx.getBean(UserService.class);
service.saveUser("Tom");  // 自动应用AOP

// 而不是
UserService target = new UserServiceImpl();
UserService proxy = createProxy(target);  // 手动创建代理
proxy.saveUser("Tom");
```

**解决方案**：在Bean创建过程中自动创建代理

### 5.2 在哪里创建代理？

**回顾Bean创建流程**：
```
createBean(beanName) {
    1. 实例化Bean
    2. 属性注入
    3. 初始化
       ├── Aware接口
       ├── BeanPostProcessor前置处理
       ├── init-method
       └── BeanPostProcessor后置处理 ← 【在这里创建代理！】
}
```

**为什么在后置处理？**
- Bean已经完全初始化
- 属性已注入
- 初始化方法已执行
- **代理包装的是完整的Bean**

### 5.3 DefaultAdvisorAutoProxyCreator工作流程

**完整代码解析**：

```java
public class DefaultAdvisorAutoProxyCreator implements BeanPostProcessor {
    
    private BeanFactory beanFactory;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        
        // ========== 第1步：过滤AOP基础类 ==========
        if (isInfrastructureClass(bean.getClass())) {
            return bean;
        }
        // Advice、Pointcut、Advisor等不需要被代理
        
        // ========== 第2步：查找匹配的Advisor ==========
        List<Advisor> advisors = getMatchingAdvisors(bean.getClass());
        
        // getMatchingAdvisors内部流程：
        // 1. 从容器获取所有Advisor类型的Bean
        //    Map<String, Advisor> advisorBeans = 
        //        beanFactory.getBeansOfType(Advisor.class);
        //
        // 2. 遍历每个Advisor，检查是否匹配
        //    for (Advisor advisor : advisorBeans.values()) {
        //        if (canApply(advisor, bean.getClass())) {
        //            matchingAdvisors.add(advisor);
        //        }
        //    }
        //
        // 3. canApply检查逻辑：
        //    - 获取Advisor的Pointcut
        //    - 遍历目标类的所有方法
        //    - 如果有任何一个方法匹配Pointcut，返回true
        
        // ========== 第3步：创建代理 ==========
        if (!advisors.isEmpty()) {
            return createProxy(bean, advisors);
        }
        
        // createProxy内部流程：
        // ProxyFactory factory = new ProxyFactory();
        // factory.setTarget(bean);
        // for (Advisor advisor : advisors) {
        //     factory.addAdvisor(advisor);
        // }
        // return factory.getProxy();
        
        // ========== 第4步：返回 ==========
        return bean;  // 没有匹配的Advisor，返回原Bean
    }
}
```

### 5.4 canApply方法详解

**判断Advisor是否适用于目标类**：

```java
private boolean canApply(Advisor advisor, Class<?> targetClass) {
    // 1. 检查是否是PointcutAdvisor
    if (!(advisor instanceof PointcutAdvisor)) {
        return true;  // 不是PointcutAdvisor，默认适用
    }
    
    // 2. 获取切点
    PointcutAdvisor pa = (PointcutAdvisor) advisor;
    Pointcut pointcut = pa.getPointcut();
    
    // 3. 检查类中是否有方法匹配切点
    Method[] methods = targetClass.getDeclaredMethods();
    for (Method method : methods) {
        if (pointcut.matches(method, targetClass)) {
            return true;  // 有任意一个方法匹配就适用
        }
    }
    
    return false;  // 所有方法都不匹配
}
```

**示例**：
```java
// 目标类
public class UserService {
    public void saveUser(String name) {}
    public void deleteUser(int id) {}
    public void findUser(int id) {}
}

// Pointcut只匹配saveUser
NameMatchPointcut pointcut = new NameMatchPointcut();
pointcut.addMethodName("saveUser");

// 检查
canApply(advisor, UserService.class)
  ↓
遍历方法: [saveUser, deleteUser, findUser]
  ↓
pointcut.matches(saveUser, UserService.class) = true ← 匹配！
  ↓
return true  // Advisor适用于UserService
```

---

## 6. 完整执行示例

### 场景设置

```java
// 【目标对象】
public class UserServiceImpl implements UserService {
    public void saveUser(String name) {
        System.out.println("【目标】保存用户: " + name);
    }
}

// 【前置通知】
public class LoggingAdvice implements MethodBeforeAdvice {
    public void before(Method method, Object[] args, Object target) {
        System.out.println("【Before】方法: " + method.getName());
    }
}

// 【环绕通知】
public class PerformanceAdvice implements MethodInterceptor {
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println("【Around-前】开始计时");
        long start = System.currentTimeMillis();
        
        Object result = invocation.proceed();
        
        long end = System.currentTimeMillis();
        System.out.println("【Around-后】耗时: " + (end - start) + "ms");
        return result;
    }
}

// 【创建代理】
UserService target = new UserServiceImpl();

NameMatchPointcut pointcut = new NameMatchPointcut();
pointcut.addMethodName("saveUser");

ProxyFactory factory = new ProxyFactory();
factory.setTarget(target);
factory.addAdvisor(new DefaultPointcutAdvisor(pointcut, new LoggingAdvice()));
factory.addAdvisor(new DefaultPointcutAdvisor(pointcut, new PerformanceAdvice()));

UserService proxy = (UserService) factory.getProxy();
```

### 完整执行流程

**调用**：`proxy.saveUser("Tom")`

```
【步骤1】用户调用
proxy.saveUser("Tom")

【步骤2】JDK代理拦截
$Proxy0.saveUser("Tom")  // JVM生成的代理类
  ↓
JdkDynamicAopProxy.invoke(proxy, saveUserMethod, ["Tom"])

【步骤3】invoke方法内部
invoke(proxy, saveUserMethod, ["Tom"]) {
    target = UserServiceImpl实例
    
    // 3.1 获取拦截器链
    chain = advised.getInterceptors(saveUserMethod)
    
    // advised.getInterceptors内部：
    // - 遍历advisors: [Advisor1, Advisor2]
    // - Advisor1的Pointcut匹配saveUser? YES
    //   → 添加Advisor1.advice (LoggingAdvice)
    // - Advisor2的Pointcut匹配saveUser? YES  
    //   → 添加Advisor2.advice (PerformanceAdvice)
    // - 返回: [LoggingAdvice, PerformanceAdvice]
    
    chain = [LoggingAdvice, PerformanceAdvice]
    
    // 3.2 创建MethodInvocation
    invocation = new ReflectiveMethodInvocation(
        target: UserServiceImpl实例,
        method: saveUserMethod,
        args: ["Tom"],
        chain: [LoggingAdvice, PerformanceAdvice]
    )
    
    // 3.3 执行链
    return invocation.proceed()
}

【步骤4】第1次proceed调用
invocation.proceed() {
    currentIndex = -1
    currentIndex == size - 1?  // -1 == 1? NO
    
    interceptor = chain[++currentIndex]  // index=0
    interceptor = LoggingAdvice (MethodBeforeAdvice)
    
    // 执行Before逻辑
    LoggingAdvice.before(saveUserMethod, ["Tom"], target)
    // 输出: "【Before】方法: saveUser"
    
    return proceed()  // 【递归】第2次调用
}

【步骤5】第2次proceed调用
invocation.proceed() {
    currentIndex = 0
    currentIndex == size - 1?  // 0 == 1? NO
    
    interceptor = chain[++currentIndex]  // index=1
    interceptor = PerformanceAdvice (MethodInterceptor)
    
    // 调用Around逻辑
    return PerformanceAdvice.invoke(this)
}

【步骤6】PerformanceAdvice.invoke内部
invoke(invocation) {
    System.out.println("【Around-前】开始计时")
    long start = currentTimeMillis()
    
    result = invocation.proceed()  // 【递归】第3次调用
    
    long end = currentTimeMillis()
    System.out.println("【Around-后】耗时: " + (end - start) + "ms")
    return result
}

【步骤7】第3次proceed调用
invocation.proceed() {
    currentIndex = 1
    currentIndex == size - 1?  // 1 == 1? YES! 到达终点
    
    return invokeJoinpoint()  // 调用目标方法
}

【步骤8】调用目标方法
invokeJoinpoint() {
    return method.invoke(target, args)
    // target.saveUser("Tom")
    // 输出: "【目标】保存用户: Tom"
    return null
}

【步骤9】逐层返回
第3次proceed → 返回null
  ↓
PerformanceAdvice.invoke → 输出"【Around-后】耗时"，返回null
  ↓
第2次proceed → 返回null
  ↓
第1次proceed → 返回null
  ↓
invoke → 返回null
  ↓
用户得到返回值: null
```

**完整输出**：
```
【Before】方法: saveUser
【Around-前】开始计时
【目标】保存用户: Tom
【Around-后】耗时: 2ms
```

---

## 7. 与IoC容器的集成

### 7.1 代理何时创建？

**在Bean的生命周期中**：

```java
createBean(beanName) {
    // 1. 实例化
    Object bean = instantiateBean(bd);
    // bean = new UserServiceImpl()
    
    // 2. 属性注入
    populateBean(bean, bd);
    
    // 3. 初始化
    bean = initializeBean(beanName, bean, bd) {
        
        invokeAwareMethods(bean);
        
        // 前置处理
        bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
        
        invokeInitMethods(bean, bd);
        
        // 【后置处理 - AOP在这里！】
        bean = applyBeanPostProcessorsAfterInitialization(bean, beanName) {
            
            for (BeanPostProcessor processor : processors) {
                bean = processor.postProcessAfterInitialization(bean, beanName);
                
                // 如果processor是DefaultAdvisorAutoProxyCreator
                if (processor instanceof DefaultAdvisorAutoProxyCreator) {
                    // 检查是否需要代理
                    // 如果需要，返回代理对象
                    bean = 代理对象
                }
            }
            
            return bean;  // 可能是代理对象，也可能是原Bean
        }
        
        return bean;
    }
    
    return bean;  // 返回的可能是代理对象
}
```

**关键**：
- Bean初始化完成后才创建代理
- 通过BeanPostProcessor机制
- 返回给容器的是代理对象
- **用户无感知！**

### 7.2 容器中存的是什么？

```java
// 创建Bean时
Object bean = createBean("userService", bd);
// 如果有匹配的Advisor，bean是代理对象
// 如果没有，bean是原对象

// 放入容器
singletonObjects.put("userService", bean);

// 用户获取
UserService service = ctx.getBean("userService", UserService.class);
// service可能是代理对象

// 用户调用
service.saveUser("Tom");
// 如果是代理，自动触发AOP逻辑
// 如果不是代理，直接执行业务逻辑
```

**总结**：
- 容器中存储的是**最终的Bean**（可能是代理，可能不是）
- 对用户完全透明
- 用户不需要知道是否有代理

### 7.3 Advisor如何注册？

**Advisor本身也是Bean**：

**方式1：XML配置**（简化）
```xml
<bean id="loggingAdvisor" class="...DefaultPointcutAdvisor">
    <!-- 需要配置Pointcut和Advice -->
</bean>
```

**方式2：注解配置**（后续可扩展）
```java
@Aspect
@Component
public class LoggingAspect {
    @Before("execution(...)")
    public void logBefore() {
        // ...
    }
}
```

**方式3：Java配置**
```java
@Configuration
public class AopConfig {
    @Bean
    public Advisor loggingAdvisor() {
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("saveUser");
        
        LoggingAdvice advice = new LoggingAdvice();
        
        return new DefaultPointcutAdvisor(pointcut, advice);
    }
}
```

---

## 8. 代码逐步解析

### 8.1 创建代理的完整代码

```java
// ========== 用户代码 ==========
UserService target = new UserServiceImpl();

// ========== 定义切点 ==========
NameMatchPointcut pointcut = new NameMatchPointcut();
pointcut.addMethodName("saveUser");
// pointcut现在会匹配所有名为"saveUser"的方法

// ========== 定义通知 ==========
MethodBeforeAdvice advice = new MethodBeforeAdvice() {
    public void before(Method method, Object[] args, Object target) {
        System.out.println("Before: " + method.getName());
    }
};

// ========== 组合Advisor ==========
Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
// advisor = {
//     pointcut: NameMatchPointcut,
//     advice: LoggingAdvice
// }

// ========== 创建代理 ==========
ProxyFactory proxyFactory = new ProxyFactory();
proxyFactory.setTarget(target);
// proxyFactory.target = UserServiceImpl实例

proxyFactory.addAdvisor(advisor);
// proxyFactory.advisors = [advisor]

UserService proxy = (UserService) proxyFactory.getProxy();

// proxyFactory.getProxy()内部：
// 1. createAopProxy() → new JdkDynamicAopProxy(this)
// 2. aopProxy.getProxy() → Proxy.newProxyInstance(...)
// 3. 返回代理对象
```

### 8.2 调用代理方法的完整代码

```java
// ========== 用户调用 ==========
proxy.saveUser("Tom");

// ========== JDK代理拦截 ==========
// 实际调用的是JVM生成的$Proxy0类的saveUser方法
$Proxy0.saveUser("Tom") {
    // 转发到InvocationHandler
    return this.h.invoke(this, saveUserMethod, new Object[]{"Tom"});
}

// ========== JdkDynamicAopProxy.invoke ==========
invoke(proxy, saveUserMethod, ["Tom"]) {
    target = UserServiceImpl实例
    
    // 1. 处理Object方法
    if (saveUserMethod.getDeclaringClass() == Object.class) {
        return method.invoke(target, args);  // 不会走到这里
    }
    
    // 2. 获取拦截器链
    chain = advised.getInterceptors(saveUserMethod);
    
    // advised.getInterceptors内部：
    // for (Advisor advisor : advisors) {
    //     if (advisor instanceof PointcutAdvisor) {
    //         PointcutAdvisor pa = (PointcutAdvisor) advisor;
    //         if (pa.getPointcut().matches(saveUserMethod, UserService.class)) {
    //             chain.add(pa.getAdvice());
    //         }
    //     }
    // }
    // 
    // pointcut.matches(saveUserMethod, ...) = true (方法名是saveUser)
    // chain = [LoggingAdvice]
    
    // 3. 检查链是否为空
    if (chain.isEmpty()) {
        return method.invoke(target, args);  // 不会走到这里
    }
    
    // 4. 创建MethodInvocation
    invocation = new ReflectiveMethodInvocation(
        target,         // UserServiceImpl实例
        saveUserMethod, // Method对象
        ["Tom"],        // 参数
        [LoggingAdvice] // 拦截器链
    );
    
    // 5. 执行拦截器链
    return invocation.proceed();
}

// ========== ReflectiveMethodInvocation.proceed ==========
proceed() {
    currentIndex = -1
    
    // 检查是否到达终点
    if (-1 == 0) { // size-1 = 1-1 = 0
        // NO，还没到
    }
    
    // 获取拦截器
    interceptor = interceptors[++currentIndex];  // index=0
    interceptor = LoggingAdvice
    
    // 判断类型
    if (LoggingAdvice instanceof MethodBeforeAdvice) {  // YES
        
        // 执行before
        LoggingAdvice.before(saveUserMethod, ["Tom"], target);
        // 输出: "【Before】方法: saveUser"
        
        // 继续执行链
        return proceed();  // 【递归调用】
    }
}

// ========== 第2次proceed调用 ==========
proceed() {
    currentIndex = 0
    
    // 检查是否到达终点
    if (0 == 0) {  // YES! 到达终点
        return invokeJoinpoint();
    }
}

// ========== 调用目标方法 ==========
invokeJoinpoint() {
    return saveUserMethod.invoke(target, ["Tom"]);
    // target.saveUser("Tom")
    // 输出: "【目标】保存用户: Tom"
    return null;
}

// ========== 返回 ==========
// invokeJoinpoint返回null
//   ↓
// 第2次proceed返回null
//   ↓
// 第1次proceed返回null  
//   ↓
// invoke返回null
//   ↓
// 用户得到null
```

**完整输出**：
```
【Before】方法: saveUser
【目标】保存用户: Tom
```

---

## 9. 核心难点解析

### 难点1：拦截器链的递归执行

**问题**：为什么要递归？

**答案**：实现灵活的执行顺序

```java
// BeforeAdvice的处理
if (interceptor instanceof MethodBeforeAdvice) {
    advice.before(...);  // 先执行before
    return proceed();    // 再继续链
}

// AfterAdvice的处理
if (interceptor instanceof AfterReturningAdvice) {
    Object result = proceed();  // 先执行链
    advice.afterReturning(result, ...);  // 再执行after
    return result;
}

// AroundAdvice的处理
if (interceptor instanceof MethodInterceptor) {
    return interceptor.invoke(this);
    // 在invoke内部控制何时调用proceed
}
```

**不同的处理方式，实现不同的执行顺序**！

### 难点2：Around通知如何包裹方法？

**Around通知的特殊性**：

```java
public class MyAroundAdvice implements MethodInterceptor {
    
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 【前置部分】
        System.out.println("Around - Before");
        
        // 【执行目标】调用proceed会继续拦截器链
        Object result = invocation.proceed();
        
        // 【后置部分】
        System.out.println("Around - After");
        
        return result;
    }
}
```

**执行流程**：
```
proceed() 获取AroundAdvice
  ↓
AroundAdvice.invoke(invocation) {
    输出: "Around - Before"
    ↓
    invocation.proceed()  ← 继续拦截器链或调用目标方法
    ↓
    输出: "Around - After"
}
```

**为什么Around最强大？**
- 可以控制是否调用 `proceed()`
- 可以在前后执行任意逻辑
- 可以修改参数和返回值
- 可以处理异常

### 难点3：多个Advisor的执行顺序

**场景**：
```java
factory.addAdvisor(advisor1);  // BeforeAdvice
factory.addAdvisor(advisor2);  // AroundAdvice
factory.addAdvisor(advisor3);  // AfterAdvice
```

**拦截器链**：`[BeforeAdvice, AroundAdvice, AfterAdvice]`

**执行顺序**：
```
BeforeAdvice.before()
  ↓
AroundAdvice.invoke() {
    Around前置
    ↓
    proceed()
      ↓
    AfterAdvice {
        proceed()
          ↓
        目标方法
          ↓
        afterReturning()
    }
      ↓
    Around后置
}
```

**实际输出**：
```
【Before】
【Around-前】
【目标方法】
【After】
【Around-后】
```

---

## 10. 关键要点总结

### 核心类的作用

| 类 | 职责 | 比喻 |
|---|------|------|
| **Pointcut** | 判断是否匹配 | 过滤器 |
| **Advice** | 定义增强逻辑 | 动作 |
| **Advisor** | 组合Pointcut和Advice | 规则+动作 |
| **ProxyFactory** | 创建代理 | 工厂 |
| **JdkDynamicAopProxy** | JDK代理实现 | 代理生成器 |
| **ReflectiveMethodInvocation** | 执行拦截器链 | 责任链协调器 |
| **DefaultAdvisorAutoProxyCreator** | 自动创建代理 | 自动化机器 |

### 执行流程总结

```
【用户调用】
proxy.method()
  ↓
【JDK代理拦截】
InvocationHandler.invoke()
  ↓
【获取拦截器链】
getInterceptors(method) → [Advice1, Advice2, ...]
  ↓
【执行拦截器链】
MethodInvocation.proceed() → 递归执行
  ↓
【调用目标方法】
method.invoke(target, args)
  ↓
【层层返回】
逐层返回结果
```

### 与IoC集成总结

```
【Bean创建】
createBean()
  ↓
【初始化】
initializeBean()
  ↓
【后置处理】
BeanPostProcessor.postProcessAfterInitialization()
  ↓
【检查Advisor】
DefaultAdvisorAutoProxyCreator
  ↓
【创建代理】
如果有匹配的Advisor → ProxyFactory.getProxy()
  ↓
【返回】
返回代理对象（或原Bean）
```

---

## 🎯 学习建议

### 1. 理解顺序

建议按以下顺序理解：
1. **JDK动态代理原理**（基础）
2. **单个Advice的执行**（简单场景）
3. **拦截器链的递归**（核心难点）
4. **自动代理创建**（集成）

### 2. 调试技巧

在这些地方打断点：
```java
// 1. 代理拦截
JdkDynamicAopProxy.invoke()

// 2. 拦截器链执行
ReflectiveMethodInvocation.proceed()

// 3. 目标方法调用
ReflectiveMethodInvocation.invokeJoinpoint()

// 4. 自动代理创建
DefaultAdvisorAutoProxyCreator.postProcessAfterInitialization()
```

**观察变量**：
- `currentInterceptorIndex` 的变化
- `interceptors` 列表的内容
- 方法调用栈

### 3. 画流程图

自己画一遍：
- 代理创建流程
- 拦截器链执行流程
- 与IoC集成流程

### 4. 运行测试观察

```bash
mvn test -Dtest=JdkProxyTest#testMultipleAdvices
```

查看控制台输出，理解执行顺序。

---

## 🤔 常见疑问

### Q1: 为什么需要MethodInvocation？

**A**: 
- 封装方法调用信息
- 提供 `proceed()` 方法让拦截器继续链
- Around通知需要它来控制目标方法执行

### Q2: 拦截器链为什么用递归？

**A**:
- 实现灵活的执行顺序
- Before可以在目标方法前执行
- After可以在目标方法后执行
- Around可以包裹目标方法

### Q3: 为什么在后置处理中创建代理？

**A**:
- Bean已完全初始化
- 代理包装的是完整的Bean
- 确保代理对象的功能完整

### Q4: 代理对象和目标对象是什么关系？

**A**:
```
代理对象（Proxy）
    ├── 实现相同的接口
    ├── 持有目标对象的引用
    └── 拦截方法调用后委托给目标对象
```

### Q5: 为什么JDK代理必须有接口？

**A**:
- JDK代理生成的是接口的实现类
- `Proxy.newProxyInstance`需要接口参数
- 如果没有接口，需要使用CGLIB（继承方式）

---

## 📊 知识图谱

```
AOP实现
├── 代理机制
│   ├── JDK动态代理
│   │   ├── Proxy.newProxyInstance
│   │   ├── InvocationHandler
│   │   └── 必须有接口
│   └── CGLIB代理（未实现）
│       ├── 继承方式
│       └── 不需要接口
│
├── 核心组件
│   ├── Pointcut（切点）
│   │   └── matches(method, class)
│   ├── Advice（通知）
│   │   ├── Before
│   │   ├── After
│   │   └── Around
│   └── Advisor（通知器）
│       └── Pointcut + Advice
│
├── 执行机制
│   ├── ReflectiveMethodInvocation
│   │   ├── 拦截器链
│   │   ├── 递归执行
│   │   └── 责任链模式
│   └── JdkDynamicAopProxy
│       ├── invoke拦截
│       └── 获取拦截器链
│
└── 集成机制
    ├── DefaultAdvisorAutoProxyCreator
    │   ├── BeanPostProcessor
    │   ├── 查找Advisor
    │   └── 创建代理
    └── 在Bean初始化后创建代理
```

---

## 🎓 总结

### AOP的本质

**代理模式 + 责任链模式 + 反射**

### 实现的关键

1. **JDK动态代理**：拦截方法调用
2. **拦截器链**：按顺序执行多个增强
3. **BeanPostProcessor**：自动创建代理
4. **Pointcut匹配**：选择性拦截

### 为什么AOP重要？

- ✅ 分离横切关注点
- ✅ 代码更清晰
- ✅ 易于维护
- ✅ 可复用的切面

### 实际应用

- 日志记录
- 性能监控
- 事务管理
- 安全控制
- 缓存管理

---

理解了这份文档，你就完全掌握了AOP的实现原理！🎉

有任何疑问随时问我！💪

