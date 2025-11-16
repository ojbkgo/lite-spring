package com.litespring.test.v2;

/**
 * 循环依赖测试类B
 * A依赖B，B依赖A（Setter注入）
 * 
 * @author lite-spring
 */
public class CircularB {
    
    private CircularA circularA;
    
    public CircularB() {
        System.out.println("CircularB实例被创建");
    }
    
    public void setCircularA(CircularA circularA) {
        System.out.println("CircularB注入CircularA");
        this.circularA = circularA;
    }
    
    public CircularA getCircularA() {
        return circularA;
    }
}

