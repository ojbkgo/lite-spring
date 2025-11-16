package com.litespring.test.v2;

/**
 * 循环依赖测试类A
 * A依赖B，B依赖A（Setter注入）
 * 
 * @author lite-spring
 */
public class CircularA {
    
    private CircularB circularB;
    
    public CircularA() {
        System.out.println("CircularA实例被创建");
    }
    
    public void setCircularB(CircularB circularB) {
        System.out.println("CircularA注入CircularB");
        this.circularB = circularB;
    }
    
    public CircularB getCircularB() {
        return circularB;
    }
}

