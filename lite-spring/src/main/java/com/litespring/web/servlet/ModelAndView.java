package com.litespring.web.servlet;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型和视图
 * 封装模型数据和视图信息
 * 
 * @author lite-spring
 */
public class ModelAndView {
    
    private Object view;  // 视图名称（String）或View对象
    private Map<String, Object> model = new HashMap<>();  // 模型数据
    
    /**
     * 创建ModelAndView（视图名称）
     */
    public ModelAndView(String viewName) {
        this.view = viewName;
    }
    
    /**
     * 创建ModelAndView（视图名称和模型）
     */
    public ModelAndView(String viewName, Map<String, Object> model) {
        this.view = viewName;
        if (model != null) {
            this.model.putAll(model);
        }
    }
    
    /**
     * 创建ModelAndView（视图名称和单个模型属性）
     */
    public ModelAndView(String viewName, String modelName, Object modelObject) {
        this.view = viewName;
        this.model.put(modelName, modelObject);
    }
    
    /**
     * 添加模型属性
     */
    public ModelAndView addObject(String attributeName, Object attributeValue) {
        this.model.put(attributeName, attributeValue);
        return this;
    }
    
    /**
     * 添加所有模型属性
     */
    public ModelAndView addAllObjects(Map<String, ?> modelMap) {
        if (modelMap != null) {
            this.model.putAll(modelMap);
        }
        return this;
    }
    
    /**
     * 判断是否是视图引用（视图名称）
     */
    public boolean isReference() {
        return this.view instanceof String;
    }
    
    /**
     * 设置视图名称
     */
    public void setViewName(String viewName) {
        this.view = viewName;
    }
    
    /**
     * 获取视图名称
     */
    public String getViewName() {
        return (this.view instanceof String ? (String) this.view : null);
    }
    
    /**
     * 获取模型数据
     */
    public Map<String, Object> getModel() {
        return this.model;
    }
}

