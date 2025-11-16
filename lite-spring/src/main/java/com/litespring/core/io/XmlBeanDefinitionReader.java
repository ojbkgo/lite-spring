package com.litespring.core.io;

import com.litespring.core.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * XML配置文件读取器
 * 负责解析XML配置文件并注册BeanDefinition
 * 
 * @author lite-spring
 */
public class XmlBeanDefinitionReader {
    
    private static final String ID_ATTRIBUTE = "id";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final String SCOPE_ATTRIBUTE = "scope";
    private static final String LAZY_INIT_ATTRIBUTE = "lazy-init";
    private static final String INIT_METHOD_ATTRIBUTE = "init-method";
    private static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
    
    // 第二阶段新增：property和constructor-arg相关常量
    private static final String PROPERTY_ELEMENT = "property";
    private static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String REF_ATTRIBUTE = "ref";
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String TYPE_ATTRIBUTE = "type";
    
    private final BeanDefinitionRegistry registry;
    
    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * 从Resource加载Bean定义
     */
    public void loadBeanDefinitions(Resource resource) {
        InputStream is = null;
        try {
            is = resource.getInputStream();
            
            // 创建DOM解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            
            // 解析Bean定义
            parseBeanDefinitions(doc.getDocumentElement());
            
        } catch (Exception e) {
            throw new BeansException("解析XML配置文件失败: " + resource.getDescription(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
        }
    }
    
    /**
     * 解析根元素下的所有Bean定义
     */
    private void parseBeanDefinitions(Element root) {
        NodeList nodeList = root.getElementsByTagName("bean");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                parseBeanDefinition((Element) node);
            }
        }
    }
    
    /**
     * 解析单个Bean元素
     */
    private void parseBeanDefinition(Element element) {
        // 获取id属性
        String id = element.getAttribute(ID_ATTRIBUTE);
        if (id == null || id.trim().isEmpty()) {
            throw new BeansException("Bean的id属性不能为空");
        }
        
        // 获取class属性
        String className = element.getAttribute(CLASS_ATTRIBUTE);
        if (className == null || className.trim().isEmpty()) {
            throw new BeansException("Bean[" + id + "]的class属性不能为空");
        }
        
        // 创建BeanDefinition
        BeanDefinition bd = new BeanDefinition(className);
        
        // 解析scope属性（可选）
        if (element.hasAttribute(SCOPE_ATTRIBUTE)) {
            bd.setScope(element.getAttribute(SCOPE_ATTRIBUTE));
        }
        
        // 解析lazy-init属性（可选）
        if (element.hasAttribute(LAZY_INIT_ATTRIBUTE)) {
            String lazyInit = element.getAttribute(LAZY_INIT_ATTRIBUTE);
            bd.setLazyInit("true".equals(lazyInit));
        }
        
        // 解析init-method属性（可选）
        if (element.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
            bd.setInitMethodName(element.getAttribute(INIT_METHOD_ATTRIBUTE));
        }
        
        // 解析destroy-method属性（可选）
        if (element.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
            bd.setDestroyMethodName(element.getAttribute(DESTROY_METHOD_ATTRIBUTE));
        }
        
        // 第二阶段新增：解析property元素
        parsePropertyElements(element, bd);
        
        // 第二阶段新增：解析constructor-arg元素
        parseConstructorArgElements(element, bd);
        
        // 注册BeanDefinition
        this.registry.registerBeanDefinition(id, bd);
    }
    
    /**
     * 解析property元素（第二阶段新增）
     */
    private void parsePropertyElements(Element beanElement, BeanDefinition bd) {
        NodeList propertyNodes = beanElement.getElementsByTagName(PROPERTY_ELEMENT);
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Node node = propertyNodes.item(i);
            if (node instanceof Element) {
                Element propertyElement = (Element) node;
                parsePropertyElement(propertyElement, bd);
            }
        }
    }
    
    /**
     * 解析单个property元素
     */
    private void parsePropertyElement(Element element, BeanDefinition bd) {
        // 获取name属性
        String name = element.getAttribute(NAME_ATTRIBUTE);
        if (name == null || name.trim().isEmpty()) {
            throw new BeansException("property元素必须指定name属性");
        }
        
        // 获取ref或value属性
        String ref = element.getAttribute(REF_ATTRIBUTE);
        String value = element.getAttribute(VALUE_ATTRIBUTE);
        
        // ref和value不能同时存在，也不能都不存在
        boolean hasRef = (ref != null && !ref.trim().isEmpty());
        boolean hasValue = (value != null && !value.trim().isEmpty());
        
        if (hasRef && hasValue) {
            throw new BeansException(
                "property元素不能同时指定ref和value属性，name=" + name
            );
        }
        
        if (!hasRef && !hasValue) {
            throw new BeansException(
                "property元素必须指定ref或value属性，name=" + name
            );
        }
        
        // 创建PropertyValue
        Object val;
        if (hasRef) {
            val = new RuntimeBeanReference(ref);
        } else {
            val = new TypedStringValue(value);
        }
        
        PropertyValue pv = new PropertyValue(name, val);
        bd.getPropertyValues().addPropertyValue(pv);
    }
    
    /**
     * 解析constructor-arg元素（第二阶段新增）
     */
    private void parseConstructorArgElements(Element beanElement, BeanDefinition bd) {
        NodeList argNodes = beanElement.getElementsByTagName(CONSTRUCTOR_ARG_ELEMENT);
        for (int i = 0; i < argNodes.getLength(); i++) {
            Node node = argNodes.item(i);
            if (node instanceof Element) {
                Element argElement = (Element) node;
                parseConstructorArgElement(argElement, bd);
            }
        }
    }
    
    /**
     * 解析单个constructor-arg元素
     */
    private void parseConstructorArgElement(Element element, BeanDefinition bd) {
        // 获取ref或value属性
        String ref = element.getAttribute(REF_ATTRIBUTE);
        String value = element.getAttribute(VALUE_ATTRIBUTE);
        String type = element.getAttribute(TYPE_ATTRIBUTE);
        
        // ref和value不能同时存在，但必须有一个
        boolean hasRef = (ref != null && !ref.trim().isEmpty());
        boolean hasValue = (value != null && !value.trim().isEmpty());
        
        if (hasRef && hasValue) {
            throw new BeansException(
                "constructor-arg元素不能同时指定ref和value属性"
            );
        }
        
        if (!hasRef && !hasValue) {
            throw new BeansException(
                "constructor-arg元素必须指定ref或value属性"
            );
        }
        
        // 创建ValueHolder
        Object val;
        if (hasRef) {
            val = new RuntimeBeanReference(ref);
        } else {
            val = new TypedStringValue(value);
        }
        
        ConstructorArgument.ValueHolder valueHolder = new ConstructorArgument.ValueHolder(val);
        if (type != null && !type.trim().isEmpty()) {
            valueHolder.setType(type);
        }
        
        bd.getConstructorArgument().addArgumentValue(valueHolder);
    }
}

