package com.litespring.core.io;

import com.litespring.core.BeanDefinition;
import com.litespring.core.BeanDefinitionRegistry;
import com.litespring.core.BeansException;
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
        
        // 注册BeanDefinition
        this.registry.registerBeanDefinition(id, bd);
    }
}

