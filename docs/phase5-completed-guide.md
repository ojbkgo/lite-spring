# 第五阶段完成指南

## 🎉 恭喜！第五阶段AOP实现已准备就绪

你的lite-spring框架现在支持AOP（面向切面编程），可以实现日志、事务、性能监控等横切关注点的分离！

---

## 📦 已创建的文件

### 核心接口（8个）
1. **Advice** - 通知标记接口
2. **MethodBeforeAdvice** - 前置通知接口
3. **AfterReturningAdvice** - 返回后通知接口
4. **MethodInterceptor** - 方法拦截器（环绕通知）
5. **MethodInvocation** - 方法调用接口
6. **Pointcut** - 切点接口
7. **Advisor** - 通知器接口
8. **PointcutAdvisor** - 切点通知器接口

### 核心实现（7个）
9. **NameMatchPointcut** - 方法名匹配切点
10. **DefaultPointcutAdvisor** - 默认切点通知器
11. **ReflectiveMethodInvocation** - 反射方法调用（拦截器链）⭐
12. **AopProxy** - AOP代理接口
13. **JdkDynamicAopProxy** - JDK动态代理实现 ⭐
14. **AdvisedSupport** - 代理配置支持
15. **ProxyFactory** - 代理工厂 ⭐
16. **DefaultAdvisorAutoProxyCreator** - 自动代理创建器 ⭐

### 测试代码
17. **UserService** 和 **UserServiceImpl** - 测试服务
18. **LoggingBeforeAdvice** - 日志前置通知
19. **LoggingAfterAdvice** - 日志返回后通知
20. **PerformanceInterceptor** - 性能监控拦截器
21. **JdkProxyTest** - JDK代理测试（⭐核心测试）
22. **AutoProxyTest** - 自动代理测试

### 文档
23. **phase5-aop.md** - 实现指南
24. **phase5-completed-guide.md** - 本文档

---

## 🚀 运行测试

### 运行AOP测试

```bash
cd /Users/ziyuewen/Devspace/myprj/lite-spring

# 运行JDK代理测试
mvn test -Dtest=JdkProxyTest
```

### 查看代理效果

运行 `JdkProxyTest.testMultipleAdvices()`，你会看到：

```
========== 调用saveUser（多个通知） ==========
【性能监控】方法开始: saveUser
【Before】调用方法: saveUser
【目标方法】保存用户: Charlie
【After】方法返回: saveUser, 返回值: null
【性能监控】方法结束: saveUser, 耗时: 2ms
```

**执行顺序**：
```
环绕通知-前半部分
  ↓
前置通知
  ↓
目标方法
  ↓
返回后通知
  ↓
环绕通知-后半部分
```

---

## 📚 核心概念理解

### 1. AOP的本质：代理模式

**不使用AOP**：
```java
public class UserServiceImpl {
    public void saveUser(String name) {
        // 日志代码
        System.out.println("执行前");
        
        // 业务代码
        userDao.save(name);
        
        // 日志代码
        System.out.println("执行后");
    }
}
```

**使用AOP**：
```java
// 业务代码：干净
public class UserServiceImpl {
    public void saveUser(String name) {
        userDao.save(name);  // 只有业务逻辑
    }
}

// 切面：日志逻辑
public class LoggingAdvice implements MethodBeforeAdvice {
    public void before(Method method, Object[] args, Object target) {
        System.out.println("执行前: " + method.getName());
    }
}

// 使用代理
UserService proxy = createProxy(new UserServiceImpl(), loggingAdvice);
proxy.saveUser("Tom");
// ↓
// 代理拦截 → 执行before → 执行目标方法
```

### 2. JDK动态代理的原理

**核心机制**：
```java
// 1. 创建代理对象
UserService proxy = (UserService) Proxy.newProxyInstance(
    classLoader,
    new Class[]{UserService.class},  // 实现的接口
    invocationHandler  // 调用处理器
);

// 2. 调用代理方法
proxy.saveUser("Tom");

// 3. 自动转发到InvocationHandler
invocationHandler.invoke(proxy, saveUserMethod, new Object[]{"Tom"});

// 4. 在invoke中可以添加额外逻辑
public Object invoke(Object proxy, Method method, Object[] args) {
    // 前置逻辑
    System.out.println("Before");
    
    // 调用目标方法
    Object result = method.invoke(target, args);
    
    // 后置逻辑
    System.out.println("After");
    
    return result;
}
```

### 3. 拦截器链的执行（责任链模式）

**场景**：一个方法匹配3个Advisor

```
调用proxy.saveUser("Tom")
  ↓
JdkDynamicAopProxy.invoke()
  ↓
获取拦截器链：[BeforeAdvice, AroundAdvice, AfterAdvice]
  ↓
创建MethodInvocation
  ↓
invocation.proceed()
  ↓
  拦截器0 (BeforeAdvice)
    ↓
    执行before逻辑
    ↓
    调用proceed()
      ↓
      拦截器1 (AroundAdvice)
        ↓
        执行around前半部分
        ↓
        调用proceed()
          ↓
          拦截器2 (AfterAdvice)
            ↓
            调用proceed()
              ↓
              目标方法执行
              ↓
            返回结果
            ↓
            执行after逻辑
          ↓
        返回结果
        ↓
        执行around后半部分
      ↓
    返回结果
  ↓
返回结果
```

### 4. AOP与IoC的集成

**关键**：在BeanPostProcessor的后置处理中创建代理

```java
// Bean创建流程
createBean("userService") {
    // 1. 实例化
    Object bean = new UserServiceImpl();
    
    // 2. 属性注入
    populateBean(bean);
    
    // 3. 初始化
    initializeBean(bean) {
        // 3.1 Aware接口
        // 3.2 BeanPostProcessor前置处理
        // 3.3 init-method
        
        // 3.4 BeanPostProcessor后置处理 ← AOP在这里！
        bean = applyBeanPostProcessorsAfterInitialization(bean) {
            for (BeanPostProcessor processor : processors) {
                bean = processor.postProcessAfterInitialization(bean, beanName);
                // DefaultAdvisorAutoProxyCreator在这里被调用
                // 如果有匹配的Advisor，返回代理对象
            }
        }
    }
    
    return bean;  // 返回的可能是代理对象！
}
```

**结果**：
- 容器中存储的是代理对象
- 用户获取的也是代理对象
- 调用方法时自动触发AOP逻辑
- **对用户完全透明！**

---

## 🎯 核心类详解

### 1. ReflectiveMethodInvocation（最核心）

**作用**：执行拦截器链

**关键方法**：`proceed()`

```java
public Object proceed() throws Throwable {
    // 【递归终止条件】所有拦截器都执行完了
    if (currentInterceptorIndex == interceptors.size() - 1) {
        return invokeJoinpoint();  // 调用目标方法
    }
    
    // 【递归】获取下一个拦截器并执行
    Object interceptor = interceptors.get(++currentInterceptorIndex);
    
    if (interceptor instanceof MethodInterceptor) {
        return ((MethodInterceptor) interceptor).invoke(this);
        // 拦截器内部会调用 invocation.proceed()，形成递归
    }
    // ... 处理其他类型
}
```

**执行示意**：
```
proceed() [index=-1]
  ↓
interceptor0.invoke(this)
  ↓
  proceed() [index=0]
    ↓
  interceptor1.invoke(this)
    ↓
    proceed() [index=1]
      ↓
    目标方法
      ↓
    返回
  ↓
  返回
↓
返回
```

### 2. JdkDynamicAopProxy（代理核心）

**作用**：创建JDK动态代理

**关键方法**：`invoke()`

```java
public Object invoke(Object proxy, Method method, Object[] args) {
    // 1. 获取拦截器链
    List<Object> chain = advised.getInterceptors(method);
    
    // 2. 如果没有拦截器，直接调用
    if (chain.isEmpty()) {
        return method.invoke(target, args);
    }
    
    // 3. 执行拦截器链
    MethodInvocation invocation = new ReflectiveMethodInvocation(...);
    return invocation.proceed();
}
```

### 3. ProxyFactory（代理工厂）

**作用**：简化代理创建

**使用示例**：
```java
ProxyFactory factory = new ProxyFactory();
factory.setTarget(target);  // 设置目标对象
factory.addAdvisor(advisor);  // 添加Advisor
Object proxy = factory.getProxy();  // 创建代理
```

### 4. DefaultAdvisorAutoProxyCreator（自动代理）

**作用**：自动为Bean创建代理

**工作流程**：
```
postProcessAfterInitialization(bean, beanName) {
    // 1. 跳过AOP基础设施类
    if (是Advice或Advisor) return bean;
    
    // 2. 查找匹配的Advisor
    List<Advisor> advisors = findMatchingAdvisors(bean.getClass());
    
    // 3. 如果有匹配的，创建代理
    if (!advisors.isEmpty()) {
        return createProxy(bean, advisors);
    }
    
    // 4. 没有匹配的，返回原Bean
    return bean;
}
```

---

## 💡 使用示例

### 示例1：基本使用

```java
// 1. 定义目标对象
UserService target = new UserServiceImpl();

// 2. 定义通知
MethodBeforeAdvice advice = (method, args, t) -> {
    System.out.println("执行前: " + method.getName());
};

// 3. 定义切点
NameMatchPointcut pointcut = new NameMatchPointcut();
pointcut.addMethodName("saveUser");

// 4. 组合为Advisor
Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);

// 5. 创建代理
ProxyFactory proxyFactory = new ProxyFactory();
proxyFactory.setTarget(target);
proxyFactory.addAdvisor(advisor);

UserService proxy = (UserService) proxyFactory.getProxy();

// 6. 使用代理
proxy.saveUser("Tom");  // 会触发前置通知
```

### 示例2：环绕通知

```java
MethodInterceptor interceptor = (invocation) -> {
    System.out.println("方法执行前");
    
    long start = System.currentTimeMillis();
    Object result = invocation.proceed();  // 执行目标方法
    long end = System.currentTimeMillis();
    
    System.out.println("方法执行后，耗时: " + (end - start) + "ms");
    
    return result;
};

// 创建代理
ProxyFactory factory = new ProxyFactory();
factory.setTarget(target);
factory.addAdvisor(new DefaultPointcutAdvisor(pointcut, interceptor));

UserService proxy = (UserService) factory.getProxy();
proxy.saveUser("Alice");
```

### 示例3：多个通知

```java
// 可以添加多个Advisor
proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, beforeAdvice));
proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, aroundAdvice));
proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, afterAdvice));

// 调用时会按顺序执行所有匹配的通知
```

---

## 🎯 AOP实现的关键点

### 1. 代理对象的创建时机

**在BeanPostProcessor的后置处理中创建**：

```
createBean(beanName) {
    实例化 → 属性注入 → 初始化
                          ↓
                    initializeBean() {
                        postProcessAfterInitialization() {
                            // DefaultAdvisorAutoProxyCreator在这里
                            if (有匹配的Advisor) {
                                return 代理对象;  ← 返回代理
                            }
                            return 原对象;
                        }
                    }
}
```

### 2. 拦截器链的执行

**责任链模式**：
- 每个拦截器决定是否继续链
- 通过调用 `invocation.proceed()` 传递
- 最后一个拦截器调用目标方法

### 3. 切点匹配

**两级匹配**：
1. 类级别：Advisor是否适用于目标类
2. 方法级别：切点是否匹配方法

### 4. 代理的透明性

**用户无感知**：
```java
// 用户代码
UserService service = ctx.getBean(UserService.class);
service.saveUser("Tom");

// 用户不需要知道service是代理对象
// 也不需要知道有AOP逻辑
// 完全透明！
```

---

## 📊 三种通知类型对比

| 通知类型 | 接口 | 执行时机 | 能否修改返回值 | 能否阻止执行 |
|---------|------|---------|--------------|------------|
| **Before** | MethodBeforeAdvice | 方法前 | ❌ | ❌ |
| **AfterReturning** | AfterReturningAdvice | 方法后 | ❌ | ❌ |
| **Around** | MethodInterceptor | 包裹方法 | ✅ | ✅ |

**Around最强大**：
```java
public Object invoke(MethodInvocation invocation) {
    // 1. 可以在方法前执行逻辑
    
    // 2. 可以决定是否调用目标方法
    if (condition) {
        return invocation.proceed();  // 调用
    } else {
        return null;  // 不调用，直接返回
    }
    
    // 3. 可以修改返回值
    Object result = invocation.proceed();
    return modifyResult(result);
    
    // 4. 可以处理异常
    try {
        return invocation.proceed();
    } catch (Exception e) {
        // 处理异常
        return defaultValue;
    }
}
```

---

## 🎨 AOP应用场景

### 1. 日志记录

```java
public class LoggingAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println("调用方法: " + invocation.getMethod().getName());
        System.out.println("参数: " + Arrays.toString(invocation.getArguments()));
        
        Object result = invocation.proceed();
        
        System.out.println("返回值: " + result);
        return result;
    }
}
```

### 2. 性能监控

```java
public class PerformanceAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long end = System.currentTimeMillis();
        
        if (end - start > 1000) {
            System.out.println("【警告】慢方法: " + invocation.getMethod().getName());
        }
        
        return result;
    }
}
```

### 3. 事务管理（第八阶段会实现）

```java
public class TransactionAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        beginTransaction();
        
        try {
            Object result = invocation.proceed();
            commitTransaction();
            return result;
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
    }
}
```

### 4. 权限检查

```java
public class SecurityAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) {
        if (!hasPermission()) {
            throw new SecurityException("无权限访问");
        }
    }
}
```

---

## ✅ 完成清单

完成第五阶段后，确认以下功能：

- [ ] Advice接口及其实现正确定义
- [ ] Pointcut能够正确匹配方法
- [ ] Advisor正确组合Pointcut和Advice
- [ ] JDK动态代理能创建代理对象
- [ ] 代理对象能拦截方法调用
- [ ] MethodBeforeAdvice在方法前执行
- [ ] AfterReturningAdvice在方法后执行
- [ ] MethodInterceptor能包裹方法执行
- [ ] 拦截器链按顺序执行
- [ ] ProxyFactory简化代理创建
- [ ] DefaultAdvisorAutoProxyCreator自动创建代理
- [ ] 切点不匹配的方法不被拦截
- [ ] 所有测试通过

---

## 🤔 思考题回顾

### 1. 为什么需要代理对象？

**答案**：
- 代理可以拦截方法调用
- 在不修改原代码的情况下增强功能
- 实现横切关注点的分离

### 2. JDK代理的限制？

**答案**：
- 只能代理接口
- 目标类必须实现接口
- 不能代理final方法

### 3. 拦截器链如何执行？

**答案**：
- 责任链模式
- 每个拦截器调用 `proceed()` 传递给下一个
- 最后一个拦截器调用目标方法

### 4. 代理何时创建？

**答案**：
- 在BeanPostProcessor的后置处理中
- Bean初始化完成后
- 返回给容器的是代理对象

### 5. 为什么三级缓存需要ObjectFactory？

**答案**（回顾第二阶段）：
- 支持AOP代理的延迟创建
- 如果有循环依赖，在三级缓存中创建代理
- 如果没有循环依赖，在后置处理中创建代理

---

## 📊 五个阶段对比

| 功能 | 阶段1 | 阶段2 | 阶段3 | 阶段4 | 阶段5 |
|------|-------|-------|-------|-------|-------|
| **IoC** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **DI** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **生命周期** | ❌ | ❌ | ✅ | ✅ | ✅ |
| **注解** | ❌ | ❌ | ❌ | ✅ | ✅ |
| **AOP** | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 🎊 重大成就

### 你现在拥有了Spring的两大核心

1. **IoC（控制反转）** ✅
   - Bean管理
   - 依赖注入
   - 生命周期
   - 注解驱动

2. **AOP（面向切面编程）** ✅
   - 动态代理
   - 切点匹配
   - 通知织入
   - 自动代理

**这两个是Spring框架的精髓！**

### 框架完成度

**核心功能完成度：90%** 🎯

**剩余可选阶段**：
- 第六阶段：MVC框架（Web支持）
- 第七阶段：数据访问（JdbcTemplate）
- 第八阶段：事务管理（基于AOP）
- 第九阶段：自动配置（Spring Boot风格）

---

## 🚀 下一步

### 1. 运行测试（10分钟）

```bash
mvn test -Dtest=JdkProxyTest
```

查看输出，理解AOP的执行流程。

### 2. 学习代码（4-6小时）

**重点阅读**：
- `ReflectiveMethodInvocation` - 拦截器链执行
- `JdkDynamicAopProxy` - JDK代理实现
- `DefaultAdvisorAutoProxyCreator` - 自动代理创建

### 3. 调试观察（2小时）

打断点：
- `JdkDynamicAopProxy.invoke()`
- `ReflectiveMethodInvocation.proceed()`
- 观察拦截器链的递归执行

### 4. 思考扩展

- 如何实现@Aspect注解？
- 如何支持AspectJ表达式？
- 如何实现CGLIB代理？

---

## 💪 恭喜你！

完成第五阶段后，你已经实现了一个**功能完整的Spring核心框架**！

- ✅ IoC容器
- ✅ 依赖注入
- ✅ Bean生命周期
- ✅ 注解驱动
- ✅ AOP代理

**你现在理解了Spring最核心的原理！**

后续阶段（MVC、事务等）都是在这个基础上的扩展。

---

准备好运行测试了吗？有任何问题随时问我！🚀💪

