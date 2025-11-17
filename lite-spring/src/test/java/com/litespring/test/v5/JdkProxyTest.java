package com.litespring.test.v5;

import com.litespring.aop.*;
import com.litespring.test.v5.advice.LoggingBeforeAdvice;
import com.litespring.test.v5.advice.LoggingAfterAdvice;
import com.litespring.test.v5.advice.PerformanceInterceptor;
import com.litespring.test.v5.service.UserService;
import com.litespring.test.v5.service.UserServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JDK动态代理测试
 * 
 * @author lite-spring
 */
public class JdkProxyTest {
    
    /**
     * 测试：创建简单的JDK代理
     */
    @Test
    public void testSimpleJdkProxy() {
        // 1. 创建目标对象
        UserService target = new UserServiceImpl();
        
        // 2. 创建通知
        LoggingBeforeAdvice advice = new LoggingBeforeAdvice();
        
        // 3. 创建切点（匹配所有方法）
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("saveUser");
        pointcut.addMethodName("findUser");
        
        // 4. 创建Advisor
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
        
        // 5. 创建代理
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisor(advisor);
        
        UserService proxy = (UserService) proxyFactory.getProxy();
        
        // 6. 调用代理方法
        proxy.saveUser("Tom");
        
        // 7. 验证通知被调用
        assertTrue(advice.getLogs().size() > 0);
        assertTrue(advice.getLogs().get(0).contains("saveUser"));
    }
    
    /**
     * 测试：前置通知
     */
    @Test
    public void testBeforeAdvice() {
        UserService target = new UserServiceImpl();
        LoggingBeforeAdvice advice = new LoggingBeforeAdvice();
        
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("saveUser");
        
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, advice));
        
        UserService proxy = (UserService) proxyFactory.getProxy();
        
        System.out.println("\n========== 调用saveUser ==========");
        proxy.saveUser("Alice");
        
        // 验证before通知被调用
        assertEquals(1, advice.getLogs().size());
        assertTrue(advice.getLogs().get(0).contains("saveUser"));
    }
    
    /**
     * 测试：返回后通知
     */
    @Test
    public void testAfterReturningAdvice() {
        UserService target = new UserServiceImpl();
        LoggingAfterAdvice advice = new LoggingAfterAdvice();
        
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("findUser");
        
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, advice));
        
        UserService proxy = (UserService) proxyFactory.getProxy();
        
        System.out.println("\n========== 调用findUser ==========");
        String result = proxy.findUser(123);
        
        // 验证返回值正确
        assertEquals("User-123", result);
        
        // 验证after通知被调用
        assertEquals(1, advice.getLogs().size());
        assertTrue(advice.getLogs().get(0).contains("User-123"));
    }
    
    /**
     * 测试：环绕通知
     */
    @Test
    public void testAroundAdvice() {
        UserService target = new UserServiceImpl();
        PerformanceInterceptor interceptor = new PerformanceInterceptor();
        
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("saveUser");
        
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, interceptor));
        
        UserService proxy = (UserService) proxyFactory.getProxy();
        
        System.out.println("\n========== 调用saveUser（环绕通知） ==========");
        proxy.saveUser("Bob");
        
        // 环绕通知应该在方法前后都执行
        // 从控制台输出可以看到
    }
    
    /**
     * 测试：多个通知
     */
    @Test
    public void testMultipleAdvices() {
        UserService target = new UserServiceImpl();
        
        LoggingBeforeAdvice beforeAdvice = new LoggingBeforeAdvice();
        LoggingAfterAdvice afterAdvice = new LoggingAfterAdvice();
        PerformanceInterceptor aroundAdvice = new PerformanceInterceptor();
        
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("saveUser");
        
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, beforeAdvice));
        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, aroundAdvice));
        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, afterAdvice));
        
        UserService proxy = (UserService) proxyFactory.getProxy();
        
        System.out.println("\n========== 调用saveUser（多个通知） ==========");
        proxy.saveUser("Charlie");
        
        // 验证所有通知都被调用
        assertTrue(beforeAdvice.getLogs().size() > 0);
        assertTrue(afterAdvice.getLogs().size() > 0);
    }
    
    /**
     * 测试：切点不匹配的方法不被拦截
     */
    @Test
    public void testPointcutNotMatch() {
        UserService target = new UserServiceImpl();
        LoggingBeforeAdvice advice = new LoggingBeforeAdvice();
        
        NameMatchPointcut pointcut = new NameMatchPointcut();
        pointcut.addMethodName("saveUser");  // 只匹配saveUser
        
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, advice));
        
        UserService proxy = (UserService) proxyFactory.getProxy();
        
        System.out.println("\n========== 调用deleteUser（不匹配） ==========");
        proxy.deleteUser(999);
        
        // deleteUser不匹配切点，不应该被拦截
        assertEquals(0, advice.getLogs().size());
        
        System.out.println("\n========== 调用saveUser（匹配） ==========");
        proxy.saveUser("David");
        
        // saveUser匹配切点，应该被拦截
        assertEquals(1, advice.getLogs().size());
    }
}

