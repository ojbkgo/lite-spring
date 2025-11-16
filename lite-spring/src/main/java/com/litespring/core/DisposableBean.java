package com.litespring.core;

/**
 * 可销毁Bean接口
 * 实现此接口的Bean在容器关闭时会调用destroy方法
 * 
 * @author lite-spring
 */
public interface DisposableBean {
    
    /**
     * 在Bean销毁时调用
     * 用于释放资源
     * 
     * @throws Exception 销毁过程中的异常
     */
    void destroy() throws Exception;
}

