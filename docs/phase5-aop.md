# 第五阶段：AOP面向切面编程实现指南

## 🎯 阶段目标

实现AOP（Aspect-Oriented Programming）面向切面编程，支持：
- JDK动态代理
- CGLIB代理（可选，第五阶段可简化）
- 切点表达式匹配
- 前置通知（Before Advice）
- 后置通知（After Advice）
- 环绕通知（Around Advice）
- 返回后通知（AfterReturning）
- 异常通知（AfterThrowing）
- 通过BeanPostProcessor自动创建代理

完成后，你将能够：
```java
// 定义切面
@Aspect
@Component
public class LoggingAspect {
    
    @Before("execution(* com.litespring.demo.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("执行前: " + joinPoint.getSignature());
    }
    
    @Around("execution(* com.litespring.demo.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long end = System.currentTimeMillis();
        System.out.println("执行耗时: " + (end - start) + "ms");
        return result;
    }
}

// 使用：完全透明，自动创建代理
UserService service = ctx.getBean(UserService.class);
service.saveUser("Tom");  // 会自动触发切面逻辑
```

---

## 📚 理论基础

### 什么是AOP？

**面向切面编程（Aspect-Oriented Programming）**：
- 是对OOP（面向对象编程）的补充
- 用于处理系统中的横切关注点
- 将横切逻辑从业务逻辑中分离

### 什么是横切关注点？

**横切关注点**：跨越多个模块的关注点

**示例**：
```java
public class UserService {
    public void saveUser(String name) {
        // 1. 日志 ← 横切关注点
        logger.info("开始保存用户");
        
        // 2. 权限检查 ← 横切关注点
        checkPermission();
        
        // 3. 事务开启 ← 横切关注点
        beginTransaction();
        
        try {
            // 4. 业务逻辑 ← 核心关注点
            userDao.save(name);
            
            // 5. 事务提交 ← 横切关注点
            commitTransaction();
        } catch (Exception e) {
            // 6. 事务回滚 ← 横切关注点
            rollbackTransaction();
            throw e;
        }
        
        // 7. 日志 ← 横切关注点
        logger.info("保存用户完成");
    }
}
```

**问题**：
- 业务代码和横切逻辑混在一起
- 日志、事务等代码重复出现
- 修改横切逻辑需要改很多地方

**AOP解决方案**：
```java
// 业务代码：只关注核心逻辑
public class UserService {
    public void saveUser(String name) {
        userDao.save(name);  // 简洁！
    }
}

// 切面：统一处理横切关注点
@Aspect
public class TransactionAspect {
    
    @Around("execution(* com.example.service.*.*(..))")
    public Object handleTransaction(ProceedingJoinPoint pjp) throws Throwable {
        beginTransaction();
        try {
            Object result = pjp.proceed();
            commitTransaction();
            return result;
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
    }
}
```

---

## 🔑 AOP核心概念

### 1. 连接点（JoinPoint）

**定义**：程序执行过程中的某个点

**示例**：
- 方法调用
- 方法执行
- 字段访问
- 异常抛出

**在Spring AOP中**：主要是方法执行

```java
// 这是一个连接点
public void saveUser(String name) {
    // ...
}
```

### 2. 切点（Pointcut）

**定义**：匹配连接点的表达式

**示例**：
```java
// 匹配UserService的所有方法
execution(* com.example.service.UserService.*(..))

// 匹配所有Service类的所有方法
execution(* com.example.service.*.*(..))

// 匹配所有public方法
execution(public * *(..))
```

**组成部分**：
```
execution(修饰符? 返回值 包名.类名.方法名(参数) 异常?)

示例：
execution(public String com.example.UserService.findById(int))

简化：
execution(* com.example..*.*(..))
```

### 3. 通知（Advice）

**定义**：在切点处执行的动作

**类型**：

#### Before（前置通知）
```java
@Before("execution(* com.example.service.*.*(..))")
public void logBefore(JoinPoint jp) {
    System.out.println("执行前: " + jp.getSignature());
}
```

#### After（后置通知，无论成功失败都执行）
```java
@After("execution(* com.example.service.*.*(..))")
public void logAfter(JoinPoint jp) {
    System.out.println("执行后: " + jp.getSignature());
}
```

#### AfterReturning（返回后通知）
```java
@AfterReturning(pointcut="execution(...)", returning="result")
public void logReturn(JoinPoint jp, Object result) {
    System.out.println("返回值: " + result);
}
```

#### AfterThrowing（异常通知）
```java
@AfterThrowing(pointcut="execution(...)", throwing="ex")
public void logException(JoinPoint jp, Exception ex) {
    System.out.println("异常: " + ex.getMessage());
}
```

#### Around（环绕通知，最强大）
```java
@Around("execution(* com.example.service.*.*(..))")
public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
    // 前置逻辑
    System.out.println("方法执行前");
    
    // 执行目标方法
    Object result = pjp.proceed();
    
    // 后置逻辑
    System.out.println("方法执行后");
    
    return result;
}
```

### 4. 切面（Aspect）

**定义**：切点 + 通知的组合

```java
@Aspect
@Component
public class LoggingAspect {
    
    // 定义切点
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceMethods() {}
    
    // 使用切点
    @Before("serviceMethods()")
    public void logBefore(JoinPoint jp) {
        // ...
    }
}
```

### 5. 织入（Weaving）

**定义**：将切面应用到目标对象的过程

**时机**：
- 编译期织入（AspectJ）
- 类加载期织入
- **运行期织入**（Spring AOP） ← lite-spring使用这种

**方式**：
- 通过代理实现
- 在BeanPostProcessor的后置处理中创建代理

### 6. 代理（Proxy）

**目标对象**：被代理的对象（原始对象）

**代理对象**：包装了目标对象的对象

```java
// 目标对象
UserService target = new UserServiceImpl();

// 创建代理
UserService proxy = createProxy(target);

// 调用代理的方法
proxy.saveUser("Tom");
// ↓
// 代理拦截调用
// ↓
// 执行before通知
// ↓
// 调用目标方法：target.saveUser("Tom")
// ↓
// 执行after通知
```

---

## 🎨 代理模式详解

### 静态代理 vs 动态代理

#### 静态代理（手动编写代理类）

```java
public class UserServiceProxy implements UserService {
    private UserService target;
    
    public UserServiceProxy(UserService target) {
        this.target = target;
    }
    
    @Override
    public void saveUser(String name) {
        // 前置逻辑
        System.out.println("执行前");
        
        // 调用目标方法
        target.saveUser(name);
        
        // 后置逻辑
        System.out.println("执行后");
    }
}
```

**缺点**：
- 每个类都要写代理类
- 代码重复
- 不灵活

#### 动态代理（运行时生成代理）

**JDK动态代理**：
```java
UserService target = new UserServiceImpl();

UserService proxy = (UserService) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            // 前置逻辑
            System.out.println("执行前: " + method.getName());
            
            // 调用目标方法
            Object result = method.invoke(target, args);
            
            // 后置逻辑
            System.out.println("执行后");
            
            return result;
        }
    }
);
```

**优点**：
- 运行时动态生成
- 一个InvocationHandler处理所有方法
- 灵活

**限制**：
- 只能代理接口
- 目标类必须实现接口

---

## 🏗️ 核心组件设计

### 1. AOP联盟接口（标准接口）

Spring AOP遵循AOP Alliance规范，我们也使用相同的接口：

#### Advice - 通知基础接口

```java
public interface Advice {
    // 标记接口
}
```

#### MethodBeforeAdvice - 前置通知

```java
public interface MethodBeforeAdvice extends Advice {
    /**
     * 在目标方法执行前调用
     */
    void before(Method method, Object[] args, Object target) throws Throwable;
}
```

#### AfterReturningAdvice - 返回后通知

```java
public interface AfterReturningAdvice extends Advice {
    /**
     * 在目标方法成功返回后调用
     */
    void afterReturning(Object returnValue, Method method, Object[] args, Object target) 
            throws Throwable;
}
```

#### MethodInterceptor - 方法拦截器（环绕通知）

```java
public interface MethodInterceptor extends Advice {
    /**
     * 拦截方法调用
     */
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

#### MethodInvocation - 方法调用

```java
public interface MethodInvocation {
    /**
     * 执行目标方法
     */
    Object proceed() throws Throwable;
    
    /**
     * 获取目标方法
     */
    Method getMethod();
    
    /**
     * 获取方法参数
     */
    Object[] getArguments();
    
    /**
     * 获取目标对象
     */
    Object getThis();
}
```

### 2. Pointcut - 切点

```java
public interface Pointcut {
    /**
     * 判断方法是否匹配切点
     */
    boolean matches(Method method, Class<?> targetClass);
}
```

**实现类**：
```java
// 简化版：使用方法名匹配
public class NameMatchPointcut implements Pointcut {
    private Set<String> methodNames = new HashSet<>();
    
    public void setMethodNames(String... methodNames) {
        this.methodNames.addAll(Arrays.asList(methodNames));
    }
    
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return methodNames.contains(method.getName());
    }
}

// 完整版：使用表达式匹配（可选）
public class AspectJExpressionPointcut implements Pointcut {
    private String expression;
    
    public void setExpression(String expression) {
        this.expression = expression;
        // 解析表达式：execution(* com.example..*.*(..))
    }
    
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // 根据表达式匹配
        return matchExpression(method, targetClass);
    }
}
```

### 3. Advisor - 通知器

**作用**：将Pointcut和Advice组合在一起

```java
public interface Advisor {
    /**
     * 获取通知
     */
    Advice getAdvice();
}

public interface PointcutAdvisor extends Advisor {
    /**
     * 获取切点
     */
    Pointcut getPointcut();
}
```

**实现**：
```java
public class DefaultPointcutAdvisor implements PointcutAdvisor {
    
    private Pointcut pointcut;
    private Advice advice;
    
    public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
        this.pointcut = pointcut;
        this.advice = advice;
    }
    
    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
    
    @Override
    public Advice getAdvice() {
        return advice;
    }
}
```

### 4. AopProxy - AOP代理

```java
public interface AopProxy {
    /**
     * 创建代理对象
     */
    Object getProxy();
    
    /**
     * 创建代理对象（指定类加载器）
     */
    Object getProxy(ClassLoader classLoader);
}
```

#### JdkDynamicAopProxy - JDK动态代理

```java
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private Object target;  // 目标对象
    private List<Advisor> advisors;  // 通知器列表
    
    public JdkDynamicAopProxy(Object target, List<Advisor> advisors) {
        this.target = target;
        this.advisors = advisors;
    }
    
    @Override
    public Object getProxy() {
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            this
        );
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 获取匹配的拦截器链
        List<Object> chain = getInterceptors(method);
        
        // 2. 如果没有拦截器，直接调用目标方法
        if (chain.isEmpty()) {
            return method.invoke(target, args);
        }
        
        // 3. 创建方法调用对象
        MethodInvocation invocation = new ReflectiveMethodInvocation(
            target, method, args, chain
        );
        
        // 4. 执行拦截器链
        return invocation.proceed();
    }
    
    /**
     * 获取匹配的拦截器
     */
    private List<Object> getInterceptors(Method method) {
        List<Object> interceptors = new ArrayList<>();
        
        for (Advisor advisor : advisors) {
            if (advisor instanceof PointcutAdvisor) {
                PointcutAdvisor pa = (PointcutAdvisor) advisor;
                if (pa.getPointcut().matches(method, target.getClass())) {
                    interceptors.add(pa.getAdvice());
                }
            }
        }
        
        return interceptors;
    }
}
```

### 5. AdvisedSupport - 代理配置

```java
public class AdvisedSupport {
    
    private Object target;  // 目标对象
    private Class<?> targetClass;  // 目标类
    private List<Advisor> advisors = new ArrayList<>();  // 通知器列表
    private boolean proxyTargetClass = false;  // 是否强制使用CGLIB
    
    public void addAdvisor(Advisor advisor) {
        this.advisors.add(advisor);
    }
    
    public List<Advisor> getAdvisors() {
        return this.advisors;
    }
    
    // ... getter和setter
}
```

### 6. ProxyFactory - 代理工厂

```java
public class ProxyFactory extends AdvisedSupport {
    
    /**
     * 创建代理
     */
    public Object getProxy() {
        return createAopProxy().getProxy();
    }
    
    /**
     * 创建AopProxy
     */
    private AopProxy createAopProxy() {
        // 判断使用JDK代理还是CGLIB代理
        if (shouldUseJdkProxy()) {
            return new JdkDynamicAopProxy(this);
        } else {
            return new CglibAopProxy(this);
        }
    }
    
    /**
     * 判断是否使用JDK代理
     */
    private boolean shouldUseJdkProxy() {
        // 如果有接口，使用JDK代理
        if (getTarget().getClass().getInterfaces().length > 0 && !isProxyTargetClass()) {
            return true;
        }
        // 否则使用CGLIB代理
        return false;
    }
}
```

### 7. AspectJAwareAdvisorAutoProxyCreator

**作用**：自动为Bean创建代理的BeanPostProcessor

```java
public class AspectJAwareAdvisorAutoProxyCreator implements BeanPostProcessor {
    
    private BeanFactory beanFactory;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 1. 获取所有Advisor
        List<Advisor> advisors = findEligibleAdvisors(bean.getClass());
        
        // 2. 如果有匹配的Advisor，创建代理
        if (!advisors.isEmpty()) {
            return createProxy(bean, advisors);
        }
        
        // 3. 没有匹配的，返回原Bean
        return bean;
    }
    
    /**
     * 查找匹配的Advisor
     */
    private List<Advisor> findEligibleAdvisors(Class<?> beanClass) {
        // 从容器中获取所有Advisor类型的Bean
        // 检查Pointcut是否匹配
    }
    
    /**
     * 创建代理
     */
    private Object createProxy(Object bean, List<Advisor> advisors) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(bean);
        proxyFactory.setTargetClass(bean.getClass());
        
        for (Advisor advisor : advisors) {
            proxyFactory.addAdvisor(advisor);
        }
        
        return proxyFactory.getProxy();
    }
}
```

---

## 📋 实现步骤

### 步骤1：创建AOP基础接口

**任务**：定义AOP的核心接口

需要创建：
1. `Advice` - 通知标记接口
2. `MethodBeforeAdvice` - 前置通知
3. `AfterReturningAdvice` - 返回后通知
4. `MethodInterceptor` - 方法拦截器
5. `MethodInvocation` - 方法调用
6. `Pointcut` - 切点接口
7. `Advisor` - 通知器接口
8. `PointcutAdvisor` - 切点通知器

**测试思路**：
```java
@Test
public void testMethodBeforeAdvice() {
    MethodBeforeAdvice advice = (method, args, target) -> {
        System.out.println("Before: " + method.getName());
    };
    
    // 测试调用
    advice.before(method, args, target);
}
```

---

### 步骤2：实现Pointcut切点

**任务**：实现简单的切点匹配

**第四阶段简化**：使用方法名匹配
**后续扩展**：使用AspectJ表达式

```java
public class NameMatchPointcut implements Pointcut {
    
    private Set<String> methodNames = new HashSet<>();
    
    public void addMethodName(String methodName) {
        this.methodNames.add(methodName);
    }
    
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return methodNames.contains(method.getName());
    }
}
```

**测试思路**：
```java
@Test
public void testPointcutMatching() {
    NameMatchPointcut pointcut = new NameMatchPointcut();
    pointcut.addMethodName("saveUser");
    
    Method method = UserService.class.getMethod("saveUser", String.class);
    assertTrue(pointcut.matches(method, UserService.class));
}
```

---

### 步骤3：实现JDK动态代理

**任务**：使用JDK的Proxy实现动态代理

**核心类**：
- `JdkDynamicAopProxy` - JDK代理实现
- `ReflectiveMethodInvocation` - 方法调用实现

**关键代码**：
```java
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private AdvisedSupport advised;
    
    public JdkDynamicAopProxy(AdvisedSupport config) {
        this.advised = config;
    }
    
    @Override
    public Object getProxy() {
        return Proxy.newProxyInstance(
            getClass().getClassLoader(),
            advised.getTarget().getClass().getInterfaces(),
            this
        );
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object target = advised.getTarget();
        
        // 获取拦截器链
        List<Object> chain = getInterceptorsAndDynamicInterceptionAdvice(method);
        
        if (chain.isEmpty()) {
            // 没有拦截器，直接调用
            return method.invoke(target, args);
        }
        
        // 创建方法调用并执行
        MethodInvocation invocation = new ReflectiveMethodInvocation(
            target, method, args, chain
        );
        
        return invocation.proceed();
    }
}
```

**测试思路**：
```java
@Test
public void testJdkProxy() {
    UserService target = new UserServiceImpl();
    
    MethodBeforeAdvice advice = (method, args, t) -> {
        System.out.println("Before: " + method.getName());
    };
    
    ProxyFactory factory = new ProxyFactory();
    factory.setTarget(target);
    factory.addAdvisor(new DefaultPointcutAdvisor(pointcut, advice));
    
    UserService proxy = (UserService) factory.getProxy();
    proxy.saveUser("Tom");
}
```

---

### 步骤4：实现拦截器链

**任务**：实现方法拦截器链的执行

**核心**：责任链模式

```java
public class ReflectiveMethodInvocation implements MethodInvocation {
    
    private Object target;  // 目标对象
    private Method method;  // 目标方法
    private Object[] args;  // 方法参数
    private List<Object> interceptors;  // 拦截器链
    private int currentInterceptorIndex = -1;  // 当前拦截器索引
    
    @Override
    public Object proceed() throws Throwable {
        // 所有拦截器都执行完了，调用目标方法
        if (currentInterceptorIndex == interceptors.size() - 1) {
            return invokeJoinpoint();
        }
        
        // 获取下一个拦截器
        Object interceptor = interceptors.get(++currentInterceptorIndex);
        
        // 执行拦截器
        if (interceptor instanceof MethodInterceptor) {
            return ((MethodInterceptor) interceptor).invoke(this);
        } else if (interceptor instanceof MethodBeforeAdvice) {
            ((MethodBeforeAdvice) interceptor).before(method, args, target);
            return proceed();  // 继续执行链
        } else if (interceptor instanceof AfterReturningAdvice) {
            Object result = proceed();  // 先执行
            ((AfterReturningAdvice) interceptor).afterReturning(result, method, args, target);
            return result;
        }
        
        return proceed();
    }
    
    private Object invokeJoinpoint() throws Throwable {
        return method.invoke(target, args);
    }
}
```

**执行流程**：
```
调用proceed()
  ↓
拦截器1.invoke(this)
  ↓
  调用proceed()
    ↓
  拦截器2.invoke(this)
    ↓
    调用proceed()
      ↓
    目标方法执行
      ↓
    返回
  ↓
  返回
↓
返回
```

---

### 步骤5：创建自动代理创建器

**任务**：自动为Bean创建代理

**核心**：在BeanPostProcessor的后置处理中创建代理

```java
public class DefaultAdvisorAutoProxyCreator implements BeanPostProcessor {
    
    private BeanFactory beanFactory;
    
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 1. 跳过AOP基础设施Bean
        if (isInfrastructureClass(bean.getClass())) {
            return bean;
        }
        
        // 2. 获取匹配的Advisor
        List<Advisor> advisors = getMatchingAdvisors(bean.getClass());
        
        // 3. 如果有匹配的，创建代理
        if (!advisors.isEmpty()) {
            return createProxy(bean, advisors);
        }
        
        return bean;
    }
    
    /**
     * 获取匹配的Advisor
     */
    private List<Advisor> getMatchingAdvisors(Class<?> beanClass) {
        List<Advisor> matchingAdvisors = new ArrayList<>();
        
        // 从容器中获取所有Advisor
        Map<String, Advisor> advisors = beanFactory.getBeansOfType(Advisor.class);
        
        for (Advisor advisor : advisors.values()) {
            if (canApply(advisor, beanClass)) {
                matchingAdvisors.add(advisor);
            }
        }
        
        return matchingAdvisors;
    }
    
    /**
     * 判断Advisor是否适用于目标类
     */
    private boolean canApply(Advisor advisor, Class<?> targetClass) {
        if (advisor instanceof PointcutAdvisor) {
            PointcutAdvisor pa = (PointcutAdvisor) advisor;
            Pointcut pointcut = pa.getPointcut();
            
            // 检查类中是否有方法匹配切点
            Method[] methods = targetClass.getDeclaredMethods();
            for (Method method : methods) {
                if (pointcut.matches(method, targetClass)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 创建代理
     */
    private Object createProxy(Object bean, List<Advisor> advisors) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(bean);
        proxyFactory.setTargetClass(bean.getClass());
        
        for (Advisor advisor : advisors) {
            proxyFactory.addAdvisor(advisor);
        }
        
        return proxyFactory.getProxy();
    }
    
    /**
     * 判断是否是AOP基础设施类
     */
    private boolean isInfrastructureClass(Class<?> beanClass) {
        return Advice.class.isAssignableFrom(beanClass) ||
               Pointcut.class.isAssignableFrom(beanClass) ||
               Advisor.class.isAssignableFrom(beanClass);
    }
}
```

---

## 🎯 AOP工作流程

### 完整流程

```
1. 定义切面
   @Aspect
   public class LoggingAspect {
       @Before("execution(...)")
       public void logBefore() {}
   }

2. 注册Advisor
   Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
   beanFactory.registerBeanDefinition("advisor", ...);

3. Bean创建过程
   UserService target = new UserServiceImpl();
   ↓
   populateBean(...)  // 属性注入
   ↓
   initializeBean(...) {
       invokeAwareMethods(...)
       ↓
       applyBeanPostProcessorsBeforeInitialization(...)
       ↓
       invokeInitMethods(...)
       ↓
       applyBeanPostProcessorsAfterInitialization(...) {
           // DefaultAdvisorAutoProxyCreator在这里！
           ↓
           查找匹配的Advisor
           ↓
           创建代理对象
           ↓
           return proxy  // 返回代理而不是原对象
       }
   }

4. 使用Bean
   UserService service = ctx.getBean(UserService.class);
   // service实际上是代理对象！
   
   service.saveUser("Tom");
   ↓
   代理拦截
   ↓
   执行before通知
   ↓
   执行目标方法
   ↓
   执行after通知
```

---

## 🤔 思考题

实现前思考这些问题：

1. **为什么需要代理对象？**
   - 不用代理能实现AOP吗？

2. **JDK代理和CGLIB代理的区别？**
   - 什么时候用JDK，什么时候用CGLIB？

3. **拦截器链是如何执行的？**
   - 如何实现Before、After、Around的不同语义？

4. **代理对象是什么时候创建的？**
   - 为什么在BeanPostProcessor的后置处理中创建？

5. **如果一个方法匹配多个Advisor怎么办？**
   - 执行顺序如何确定？

6. **代理对象和目标对象的关系？**
   - 容器中存的是代理还是目标对象？

---

## 🎨 AOP应用场景

### 1. 日志记录

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("execution(* com.example.service.*.*(..))")
    public Object logMethod(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        
        System.out.println("【日志】执行方法: " + methodName);
        System.out.println("【日志】参数: " + Arrays.toString(pjp.getArgs()));
        
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long end = System.currentTimeMillis();
        
        System.out.println("【日志】返回值: " + result);
        System.out.println("【日志】耗时: " + (end - start) + "ms");
        
        return result;
    }
}
```

### 2. 事务管理

```java
@Aspect
@Component
public class TransactionAspect {
    
    @Around("@annotation(Transactional)")
    public Object handleTransaction(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("【事务】开启事务");
        
        try {
            Object result = pjp.proceed();
            System.out.println("【事务】提交事务");
            return result;
        } catch (Exception e) {
            System.out.println("【事务】回滚事务");
            throw e;
        }
    }
}
```

### 3. 性能监控

```java
@Aspect
@Component
public class PerformanceAspect {
    
    @Around("execution(* com.example..*.*(..))")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        
        Object result = pjp.proceed();
        
        long end = System.currentTimeMillis();
        long duration = end - start;
        
        if (duration > 1000) {
            System.out.println("【警告】慢方法: " + pjp.getSignature() + ", 耗时: " + duration + "ms");
        }
        
        return result;
    }
}
```

### 4. 异常处理

```java
@Aspect
@Component
public class ExceptionAspect {
    
    @AfterThrowing(pointcut="execution(...)", throwing="ex")
    public void handleException(JoinPoint jp, Exception ex) {
        System.err.println("【异常】方法: " + jp.getSignature());
        System.err.println("【异常】信息: " + ex.getMessage());
        // 发送告警、记录日志等
    }
}
```

---

## 📊 与第四阶段的对比

| 方面 | 第四阶段 | 第五阶段 |
|------|---------|---------|
| **依赖注入** | ✅ @Autowired | ✅ 保持 |
| **横切关注点** | ❌ 混在业务代码中 | ✅ 分离到切面 |
| **代理** | ❌ 无 | ✅ JDK动态代理 |
| **切面** | ❌ 无 | ✅ Advisor |
| **事务** | ❌ 手动 | ✅ 切面实现 |
| **日志** | ❌ 手动 | ✅ 切面实现 |

---

## ✅ 完成标志

完成第五阶段后，你应该能够：

1. ✅ 定义Pointcut切点
2. ✅ 定义各种Advice通知
3. ✅ 组合Pointcut和Advice为Advisor
4. ✅ 使用JDK动态代理创建代理对象
5. ✅ 拦截器链正确执行
6. ✅ Before通知在方法前执行
7. ✅ AfterReturning在方法后执行
8. ✅ Around通知包裹方法执行
9. ✅ 自动为Bean创建代理
10. ✅ 代理对象透明使用

---

## 🎓 学习建议

### 实现顺序

1. **基础接口** → 简单
2. **Pointcut实现** → 中等
3. **JDK动态代理** → 重点
4. **拦截器链** → 难点
5. **自动代理创建** → 组合

### 预计时间

- 理解文档：1.5-2小时
- 实现代码：6-8小时
- 测试调试：2-3小时
- **总计：10-13小时**

### 难度评估

| 阶段 | 难度 | 核心挑战 |
|------|------|---------|
| 第二阶段 | ⭐⭐⭐⭐ | 循环依赖 |
| 第四阶段 | ⭐⭐⭐⭐ | 类路径扫描 |
| **第五阶段** | **⭐⭐⭐⭐⭐** | **动态代理、拦截器链** |

**第五阶段是最难的！** 但也是最有价值的！

---

## 🚀 准备好了吗？

阅读并理解这份文档后，告诉我，我会为你提供：
- 完整的AOP接口定义
- JdkDynamicAopProxy实现
- ReflectiveMethodInvocation实现
- ProxyFactory实现
- DefaultAdvisorAutoProxyCreator实现
- 完整的测试用例

AOP是Spring的精华，完成后你将真正掌握Spring的核心！

有任何疑问随时问我！💪🚀

