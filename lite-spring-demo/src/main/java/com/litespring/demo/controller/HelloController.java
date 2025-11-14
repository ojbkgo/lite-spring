package com.litespring.demo.controller;

/**
 * Hello控制器示例
 * 演示lite-spring框架的基本使用
 * 
 * 注意：这是目标代码，当前lite-spring还未实现相关注解
 * 将在后续迭代中逐步实现
 * 
 * @author lite-spring
 */
// @RestController
// @RequestMapping("/hello")
public class HelloController {
    
    // @Autowired
    // private HelloService helloService;
    
    // @GetMapping
    public String hello() {
        return "Hello, Lite Spring!";
    }
    
    // @GetMapping("/{name}")
    public String helloWithName(/* @PathVariable */ String name) {
        // return helloService.greet(name);
        return "Hello, " + name + "!";
    }
}

