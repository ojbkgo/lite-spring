package com.litespring.test.v2;

import com.litespring.util.SimpleTypeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 类型转换测试
 * 
 * @author lite-spring
 */
public class TypeConversionTest {
    
    private SimpleTypeConverter converter;
    
    @BeforeEach
    public void setUp() {
        converter = new SimpleTypeConverter();
    }
    
    /**
     * 测试：String类型
     */
    @Test
    public void testStringConversion() {
        Object result = converter.convertIfNecessary("Hello", String.class);
        assertEquals("Hello", result);
        assertTrue(result instanceof String);
    }
    
    /**
     * 测试：int类型
     */
    @Test
    public void testIntConversion() {
        Object result = converter.convertIfNecessary("123", int.class);
        assertEquals(123, result);
        assertTrue(result instanceof Integer);
    }
    
    /**
     * 测试：Integer类型
     */
    @Test
    public void testIntegerConversion() {
        Object result = converter.convertIfNecessary("456", Integer.class);
        assertEquals(456, result);
        assertTrue(result instanceof Integer);
    }
    
    /**
     * 测试：boolean类型（true）
     */
    @Test
    public void testBooleanTrueConversion() {
        assertTrue((Boolean) converter.convertIfNecessary("true", boolean.class));
        assertTrue((Boolean) converter.convertIfNecessary("yes", boolean.class));
        assertTrue((Boolean) converter.convertIfNecessary("1", boolean.class));
        assertTrue((Boolean) converter.convertIfNecessary("on", boolean.class));
    }
    
    /**
     * 测试：boolean类型（false）
     */
    @Test
    public void testBooleanFalseConversion() {
        assertFalse((Boolean) converter.convertIfNecessary("false", boolean.class));
        assertFalse((Boolean) converter.convertIfNecessary("no", boolean.class));
        assertFalse((Boolean) converter.convertIfNecessary("0", boolean.class));
        assertFalse((Boolean) converter.convertIfNecessary("off", boolean.class));
    }
    
    /**
     * 测试：double类型
     */
    @Test
    public void testDoubleConversion() {
        Object result = converter.convertIfNecessary("3.14", double.class);
        assertEquals(3.14, (Double) result, 0.001);
    }
    
    /**
     * 测试：long类型
     */
    @Test
    public void testLongConversion() {
        Object result = converter.convertIfNecessary("9999999999", long.class);
        assertEquals(9999999999L, result);
    }
    
    /**
     * 测试：null值处理（包装类型）
     */
    @Test
    public void testNullValueForWrapperType() {
        Object result = converter.convertIfNecessary(null, Integer.class);
        assertNull(result);
    }
    
    /**
     * 测试：null值处理（基本类型）
     */
    @Test
    public void testNullValueForPrimitiveType() {
        Object result = converter.convertIfNecessary(null, int.class);
        assertEquals(0, result);
    }
    
    /**
     * 测试：空字符串处理
     */
    @Test
    public void testEmptyStringForPrimitiveType() {
        Object result = converter.convertIfNecessary("", int.class);
        assertEquals(0, result);
    }
    
    /**
     * 测试：转换失败
     */
    @Test
    public void testConversionFailure() {
        assertThrows(Exception.class, () -> {
            converter.convertIfNecessary("abc", int.class);
        });
    }
}

