# 第六阶段：MVC框架实现指南

## 🎯 阶段目标

实现Web MVC框架，支持：
- DispatcherServlet（前端控制器）
- HandlerMapping（处理器映射）
- HandlerAdapter（处理器适配器）
- @Controller和@RestController注解
- @RequestMapping及其变体（@GetMapping、@PostMapping等）
- @RequestParam、@PathVariable参数绑定
- @RequestBody、@ResponseBody（JSON支持）
- 视图解析（简化）
- RESTful API支持

完成后，你将能够：
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable int id) {
        return userService.findById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
    
    @GetMapping
    public List<User> listUsers(@RequestParam(defaultValue = "1") int page) {
        return userService.list(page);
    }
}
```

启动Web应用：
```java
@Configuration
@ComponentScan("com.litespring.demo")
public class WebAppConfig {
}

// 启动
public class WebApplication {
    public static void main(String[] args) throws Exception {
        // 创建嵌入式Tomcat
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.start();
    }
}
```

---

## 📚 理论基础

### 什么是MVC？

**MVC（Model-View-Controller）**：一种软件架构模式

**三个组件**：
- **Model（模型）**：业务数据和业务逻辑
- **View（视图）**：展示数据（HTML、JSON等）
- **Controller（控制器）**：处理请求，调用Model，返回View

```
浏览器
  ↓ HTTP请求
Controller（处理请求，调用Service）
  ↓
Service/Dao（业务逻辑，操作数据库）
  ↓
Model（数据）
  ↓
View（展示）
  ↓ HTTP响应
浏览器
```

### Spring MVC的核心思想

**前端控制器模式（Front Controller Pattern）**

**传统Servlet开发**：
```java
// 每个URL都要一个Servlet
public class UserServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 处理逻辑
    }
}

public class OrderServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 处理逻辑
    }
}

// web.xml配置
<servlet>
    <servlet-name>userServlet</servlet-name>
    <servlet-class>UserServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>userServlet</servlet-name>
    <url-pattern>/user</url-pattern>
</servlet-mapping>
```

**问题**：
- 每个功能都要一个Servlet
- 配置繁琐
- 代码重复

**Spring MVC方式**：
```java
// 只有一个DispatcherServlet（前端控制器）
public class DispatcherServlet extends HttpServlet {
    protected void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        // 1. 根据URL找到对应的Controller方法
        // 2. 调用方法
        // 3. 处理返回值
    }
}

// 业务逻辑用Controller
@Controller
public class UserController {
    @RequestMapping("/user")
    public String getUser() {
        return "user";
    }
}
```

**优势**：
- 只需一个Servlet
- Controller只是普通类（POJO）
- 通过注解配置
- 统一的请求处理流程

---

## 🏗️ Spring MVC工作流程

### 完整的请求处理流程

```
【1】浏览器发送请求
GET /api/users/123
  ↓
【2】DispatcherServlet接收（doGet/doPost）
  ↓
【3】HandlerMapping查找Handler
根据URL "/api/users/123" 找到对应的Controller方法
  ↓
【4】HandlerAdapter执行Handler
调用Controller方法，处理参数绑定
  ↓
【5】Controller执行业务逻辑
调用Service层，返回Model
  ↓
【6】ViewResolver解析视图
根据返回值确定如何渲染（HTML/JSON）
  ↓
【7】渲染响应
生成HTTP响应体
  ↓
【8】返回给浏览器
```

### 核心组件

```
DispatcherServlet（前端控制器）
    ├── HandlerMapping（找Handler）
    ├── HandlerAdapter（执行Handler）
    ├── ViewResolver（解析视图）
    └── HandlerExceptionResolver（异常处理）
```

---

## 🔑 核心组件设计

### 1. DispatcherServlet - 前端控制器

**作用**：
- 接收所有HTTP请求
- 协调其他组件处理请求
- 统一的异常处理

```java
public class DispatcherServlet extends HttpServlet {
    
    private HandlerMapping handlerMapping;
    private HandlerAdapter handlerAdapter;
    private ViewResolver viewResolver;
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            // 统一异常处理
            processException(req, resp, e);
        }
    }
    
    /**
     * 核心分发方法
     */
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) 
            throws Exception {
        
        // 1. 查找Handler
        HandlerExecutionChain chain = handlerMapping.getHandler(request);
        if (chain == null) {
            response.sendError(404, "Not Found");
            return;
        }
        
        // 2. 执行拦截器的前置处理
        if (!chain.applyPreHandle(request, response)) {
            return;
        }
        
        // 3. 执行Handler
        ModelAndView mv = handlerAdapter.handle(request, response, chain.getHandler());
        
        // 4. 执行拦截器的后置处理
        chain.applyPostHandle(request, response, mv);
        
        // 5. 渲染视图
        if (mv != null) {
            render(mv, request, response);
        }
    }
    
    /**
     * 渲染视图
     */
    private void render(ModelAndView mv, HttpServletRequest request, 
                       HttpServletResponse response) throws Exception {
        
        if (mv.isReference()) {
            // 视图名称 → 通过ViewResolver解析
            View view = viewResolver.resolveViewName(mv.getViewName());
            view.render(mv.getModel(), request, response);
        } else {
            // 直接是View对象
            mv.getView().render(mv.getModel(), request, response);
        }
    }
}
```

---

### 2. HandlerMapping - 处理器映射

**作用**：根据请求URL找到对应的Controller方法

**接口定义**：
```java
public interface HandlerMapping {
    /**
     * 根据请求获取Handler
     */
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
```

**实现类**：`RequestMappingHandlerMapping`

```java
public class RequestMappingHandlerMapping implements HandlerMapping {
    
    // 存储URL和Handler的映射
    // key: URL pattern, value: HandlerMethod
    private Map<RequestMappingInfo, HandlerMethod> handlerMethods = new HashMap<>();
    
    /**
     * 注册Handler方法
     */
    public void registerHandlerMethod(RequestMappingInfo info, HandlerMethod method) {
        this.handlerMethods.put(info, method);
    }
    
    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) {
        String url = request.getRequestURI();
        String method = request.getMethod();
        
        // 查找匹配的Handler
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            
            if (info.matches(url, method)) {
                HandlerMethod handler = entry.getValue();
                return new HandlerExecutionChain(handler);
            }
        }
        
        return null;  // 没找到
    }
}
```

**核心数据结构**：

#### RequestMappingInfo - 请求映射信息
```java
public class RequestMappingInfo {
    private String[] paths;        // URL路径
    private RequestMethod[] methods;  // HTTP方法（GET/POST等）
    
    public boolean matches(String url, String httpMethod) {
        // 匹配URL和HTTP方法
        return matchesPath(url) && matchesMethod(httpMethod);
    }
    
    private boolean matchesPath(String url) {
        for (String path : paths) {
            if (PathMatcher.match(path, url)) {
                return true;
            }
        }
        return false;
    }
}
```

#### HandlerMethod - 处理器方法
```java
public class HandlerMethod {
    private Object bean;        // Controller实例
    private Method method;      // Controller方法
    private MethodParameter[] parameters;  // 方法参数信息
    
    /**
     * 调用Handler方法
     */
    public Object invoke(Object... args) throws Exception {
        return method.invoke(bean, args);
    }
}
```

---

### 3. HandlerAdapter - 处理器适配器

**作用**：执行Handler，处理参数绑定和返回值

**接口定义**：
```java
public interface HandlerAdapter {
    /**
     * 是否支持该Handler
     */
    boolean supports(Object handler);
    
    /**
     * 执行Handler
     */
    ModelAndView handle(HttpServletRequest request, 
                       HttpServletResponse response,
                       Object handler) throws Exception;
}
```

**实现类**：`RequestMappingHandlerAdapter`

```java
public class RequestMappingHandlerAdapter implements HandlerAdapter {
    
    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerMethod;
    }
    
    @Override
    public ModelAndView handle(HttpServletRequest request, 
                              HttpServletResponse response,
                              Object handler) throws Exception {
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 1. 参数解析（核心！）
        Object[] args = resolveArguments(handlerMethod, request, response);
        
        // 2. 调用Controller方法
        Object returnValue = handlerMethod.invoke(args);
        
        // 3. 处理返回值
        return handleReturnValue(returnValue, handlerMethod, request, response);
    }
    
    /**
     * 解析方法参数
     */
    private Object[] resolveArguments(HandlerMethod handler, 
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        
        MethodParameter[] parameters = handler.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            
            // 根据参数类型和注解解析
            if (param.hasAnnotation(RequestParam.class)) {
                args[i] = resolveRequestParam(param, request);
            } else if (param.hasAnnotation(PathVariable.class)) {
                args[i] = resolvePathVariable(param, request);
            } else if (param.hasAnnotation(RequestBody.class)) {
                args[i] = resolveRequestBody(param, request);
            } else if (param.getType() == HttpServletRequest.class) {
                args[i] = request;
            } else if (param.getType() == HttpServletResponse.class) {
                args[i] = response;
            }
            // ... 其他类型
        }
        
        return args;
    }
    
    /**
     * 处理返回值
     */
    private ModelAndView handleReturnValue(Object returnValue, 
                                          HandlerMethod handler,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        
        // 1. 如果方法标注@ResponseBody，返回JSON
        if (handler.hasAnnotation(ResponseBody.class)) {
            writeJson(returnValue, response);
            return null;
        }
        
        // 2. 如果返回String，作为视图名
        if (returnValue instanceof String) {
            return new ModelAndView((String) returnValue);
        }
        
        // 3. 如果返回ModelAndView
        if (returnValue instanceof ModelAndView) {
            return (ModelAndView) returnValue;
        }
        
        return null;
    }
}
```

---

### 4. 注解定义

#### @Controller - 控制器注解

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // 也是组件
public @interface Controller {
    String value() default "";
}
```

#### @RestController - REST控制器

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody  // 所有方法都返回JSON
public @interface RestController {
    String value() default "";
}
```

#### @RequestMapping - 请求映射

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    /**
     * URL路径
     */
    String[] value() default {};
    
    /**
     * URL路径（与value相同）
     */
    String[] path() default {};
    
    /**
     * HTTP方法
     */
    RequestMethod[] method() default {};
}

public enum RequestMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}
```

#### @GetMapping、@PostMapping等

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.GET)
public @interface GetMapping {
    String[] value() default {};
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.POST)
public @interface PostMapping {
    String[] value() default {};
}

// PUT、DELETE、PATCH类似
```

#### @RequestParam - 请求参数

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    /**
     * 参数名称
     */
    String value() default "";
    
    /**
     * 是否必须
     */
    boolean required() default true;
    
    /**
     * 默认值
     */
    String defaultValue() default ValueConstants.DEFAULT_NONE;
}
```

#### @PathVariable - 路径变量

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {
    /**
     * 变量名称
     */
    String value() default "";
}
```

#### @RequestBody - 请求体

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
    /**
     * 是否必须
     */
    boolean required() default true;
}
```

#### @ResponseBody - 响应体

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseBody {
}
```

---

## 📋 实现步骤

### 步骤1：创建MVC核心注解

**任务**：定义8个MVC注解
1. @Controller
2. @RestController
3. @RequestMapping
4. @GetMapping、@PostMapping、@PutMapping、@DeleteMapping
5. @RequestParam
6. @PathVariable
7. @RequestBody
8. @ResponseBody

**注意**：
- @RestController包含@Controller和@ResponseBody
- @GetMapping等包含@RequestMapping

---

### 步骤2：实现HandlerMapping

**任务**：实现请求到Handler的映射

**核心类**：
- `RequestMappingInfo` - 请求映射信息
- `HandlerMethod` - 处理器方法封装
- `RequestMappingHandlerMapping` - 映射实现

**工作流程**：
```
1. 扫描所有@Controller类
2. 扫描@RequestMapping方法
3. 提取URL、HTTP方法等信息
4. 构建URL → HandlerMethod的映射表
5. 请求来时根据URL查找
```

**实现提示**：
```java
public class RequestMappingHandlerMapping implements HandlerMapping {
    
    private Map<RequestMappingInfo, HandlerMethod> handlerMethods = new HashMap<>();
    
    /**
     * 初始化：扫描Controller
     */
    public void afterPropertiesSet() {
        // 从容器获取所有@Controller bean
        Map<String, Object> controllers = beanFactory.getBeansWithAnnotation(Controller.class);
        
        for (Object controller : controllers.values()) {
            // 扫描Controller的方法
            detectHandlerMethods(controller);
        }
    }
    
    /**
     * 扫描Handler方法
     */
    private void detectHandlerMethods(Object handler) {
        Class<?> handlerType = handler.getClass();
        
        // 类级别的@RequestMapping
        RequestMapping typeMapping = handlerType.getAnnotation(RequestMapping.class);
        
        // 扫描所有方法
        Method[] methods = handlerType.getDeclaredMethods();
        for (Method method : methods) {
            RequestMapping methodMapping = method.getAnnotation(RequestMapping.class);
            if (methodMapping != null) {
                // 组合类和方法的映射信息
                RequestMappingInfo info = createMappingInfo(typeMapping, methodMapping);
                HandlerMethod handlerMethod = new HandlerMethod(handler, method);
                
                registerHandlerMethod(info, handlerMethod);
            }
        }
    }
}
```

---

### 步骤3：实现参数解析

**任务**：解析Controller方法的参数

**挑战**：
- @RequestParam - 从query参数获取
- @PathVariable - 从URL路径提取
- @RequestBody - 从请求体解析JSON
- HttpServletRequest/Response - 直接注入
- 自定义对象 - 参数绑定

**核心类**：`HandlerMethodArgumentResolver`

```java
public interface HandlerMethodArgumentResolver {
    /**
     * 是否支持该参数
     */
    boolean supportsParameter(MethodParameter parameter);
    
    /**
     * 解析参数值
     */
    Object resolveArgument(MethodParameter parameter,
                          HttpServletRequest request,
                          HttpServletResponse response) throws Exception;
}
```

**实现示例**：

#### RequestParamMethodArgumentResolver

```java
public class RequestParamMethodArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestParam.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        String name = requestParam.value();
        
        // 从request获取参数
        String value = request.getParameter(name);
        
        // 如果没有值，使用默认值
        if (value == null) {
            if (requestParam.required()) {
                throw new MissingServletRequestParameterException(name);
            }
            value = requestParam.defaultValue();
        }
        
        // 类型转换
        return convertValue(value, parameter.getParameterType());
    }
}
```

#### PathVariableMethodArgumentResolver

```java
public class PathVariableMethodArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(PathVariable.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        String name = pathVariable.value();
        
        // 从URL中提取路径变量
        // 例如：/users/{id} 匹配 /users/123，提取 id=123
        Map<String, String> uriVariables = extractUriVariables(request);
        String value = uriVariables.get(name);
        
        if (value == null) {
            throw new IllegalArgumentException("路径变量不存在: " + name);
        }
        
        return convertValue(value, parameter.getParameterType());
    }
}
```

---

### 步骤4：实现ModelAndView

**作用**：封装模型数据和视图信息

```java
public class ModelAndView {
    
    private Object view;  // 视图名称（String）或View对象
    private Map<String, Object> model = new HashMap<>();  // 模型数据
    
    public ModelAndView(String viewName) {
        this.view = viewName;
    }
    
    public ModelAndView(View view) {
        this.view = view;
    }
    
    /**
     * 添加模型属性
     */
    public ModelAndView addObject(String name, Object value) {
        this.model.put(name, value);
        return this;
    }
    
    /**
     * 是否是视图引用（视图名称）
     */
    public boolean isReference() {
        return this.view instanceof String;
    }
    
    public String getViewName() {
        if (this.view instanceof String) {
            return (String) this.view;
        }
        return null;
    }
    
    public View getView() {
        if (this.view instanceof View) {
            return (View) this.view;
        }
        return null;
    }
    
    public Map<String, Object> getModel() {
        return this.model;
    }
}
```

---

### 步骤5：实现JSON支持

**任务**：支持@ResponseBody返回JSON

**简化方案**（第六阶段）：
- 使用简单的JSON库（如Gson）
- 或手写简单的JSON序列化

**完整方案**（后续优化）：
- 集成Jackson
- 支持更多数据格式

**实现提示**：
```java
public class JsonMessageConverter {
    
    private Gson gson = new Gson();
    
    /**
     * 将对象转换为JSON
     */
    public String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * 将JSON转换为对象
     */
    public <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }
    
    /**
     * 写JSON响应
     */
    public void writeJson(Object obj, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(toJson(obj));
    }
}
```

---

### 步骤6：路径匹配

**任务**：支持路径模式匹配

**场景**：
```java
@GetMapping("/users/{id}")  // 路径模板
// 需要匹配：/users/123, /users/456 等
```

**实现**：简单的路径匹配器

```java
public class PathMatcher {
    
    /**
     * 匹配路径
     * pattern: /users/{id}
     * path: /users/123
     */
    public static boolean match(String pattern, String path) {
        // 简化实现：支持{变量}占位符
        
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        
        if (patternParts.length != pathParts.length) {
            return false;
        }
        
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            String pathPart = pathParts[i];
            
            // 如果是变量，跳过
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                continue;
            }
            
            // 普通部分，必须完全匹配
            if (!patternPart.equals(pathPart)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 提取路径变量
     * pattern: /users/{id}
     * path: /users/123
     * return: {id=123}
     */
    public static Map<String, String> extractUriVariables(String pattern, String path) {
        Map<String, String> variables = new HashMap<>();
        
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                String varName = patternPart.substring(1, patternPart.length() - 1);
                variables.put(varName, pathParts[i]);
            }
        }
        
        return variables;
    }
}
```

---

## 🎯 MVC工作流程详解

### 完整示例

**Controller定义**：
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable int id) {
        return userService.findById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
}
```

**请求处理流程**：

```
【1】浏览器请求
GET http://localhost:8080/api/users/123
  ↓
【2】Tomcat接收，转发给DispatcherServlet
DispatcherServlet.service(request, response)
  ↓
【3】DispatcherServlet.doDispatch()
  ↓
【4】HandlerMapping查找Handler
handlerMapping.getHandler(request)
  - 请求URL: /api/users/123
  - 请求方法: GET
  - 遍历handlerMethods查找匹配
  - 找到: UserController.getUser(int id)
  - 返回: HandlerExecutionChain
  ↓
【5】HandlerAdapter执行Handler
handlerAdapter.handle(request, response, handler)
  ↓
【5.1】解析参数
  - 方法参数: (int id)
  - 参数有@PathVariable注解
  - 从URL提取: id=123
  - 类型转换: "123" → 123
  - args = [123]
  ↓
【5.2】调用Controller方法
handlerMethod.invoke([123])
  - userController.getUser(123)
  - 返回: User对象
  ↓
【5.3】处理返回值
  - 方法有@ResponseBody（因为类有@RestController）
  - 将User对象转换为JSON
  - 写入response
  ↓
【6】返回响应
HTTP/1.1 200 OK
Content-Type: application/json

{"id":123,"name":"Tom","age":25}
```

---

## 🤔 关键难点

### 难点1：URL路径匹配

**问题**：如何匹配 `/users/{id}` 和 `/users/123`？

**解决**：
```java
// 1. 分割路径
pattern: [users, {id}]
path: [users, 123]

// 2. 逐段匹配
users == users ✓
{id} 是变量，匹配任意值 ✓

// 3. 提取变量
{id} → 123
```

### 难点2：参数绑定

**问题**：如何将HTTP参数绑定到方法参数？

**解决**：
```java
// 方法定义
public User getUser(@PathVariable int id, 
                   @RequestParam String name,
                   @RequestBody User user)

// 参数解析
for (参数 : 方法参数) {
    if (有@PathVariable) {
        从URL提取 → 类型转换
    } else if (有@RequestParam) {
        从query参数获取 → 类型转换
    } else if (有@RequestBody) {
        从请求体解析JSON → 反序列化
    }
}
```

### 难点3：JSON序列化

**问题**：如何将Java对象转换为JSON？

**简化方案**：
```java
// 使用Gson或其他轻量JSON库
Gson gson = new Gson();
String json = gson.toJson(user);
```

**手写方案**（可选）：
```java
// 简单的JSON序列化
public String toJson(Object obj) {
    // 反射获取字段
    // 拼接JSON字符串
}
```

### 难点4：初始化HandlerMapping

**问题**：何时扫描Controller？

**解决**：
```java
// 方式1：在DispatcherServlet初始化时
public class DispatcherServlet extends HttpServlet {
    @Override
    public void init() {
        // 初始化HandlerMapping
        handlerMapping.afterPropertiesSet();
    }
}

// 方式2：实现InitializingBean
public class RequestMappingHandlerMapping 
        implements HandlerMapping, InitializingBean {
    
    @Override
    public void afterPropertiesSet() {
        // 扫描Controller
    }
}
```

---

## 📊 与第五阶段的对比

| 方面 | 第五阶段 | 第六阶段 |
|------|---------|---------|
| **关注点** | AOP代理 | Web请求处理 |
| **核心技术** | 动态代理 | Servlet |
| **主要注解** | 无（手动配置） | @Controller、@RequestMapping |
| **应用场景** | 横切关注点 | Web应用 |
| **难度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## ✅ 完成标志

完成第六阶段后，你应该能够：

1. ✅ 使用@Controller定义控制器
2. ✅ 使用@RequestMapping映射URL
3. ✅ 使用@GetMapping、@PostMapping等快捷注解
4. ✅ 使用@PathVariable提取路径参数
5. ✅ 使用@RequestParam获取查询参数
6. ✅ 使用@RequestBody解析JSON
7. ✅ 使用@ResponseBody返回JSON
8. ✅ 启动嵌入式Tomcat
9. ✅ 开发RESTful API
10. ✅ 处理GET/POST/PUT/DELETE请求

---

## 🎓 学习建议

### 前置知识

**必须掌握**：
- Servlet基础（HttpServlet、request、response）
- HTTP协议（GET/POST、状态码）
- JSON格式

**可选了解**：
- RESTful API设计
- 前后端分离

### 实现顺序

1. **先实现注解定义**（简单）
2. **再实现HandlerMapping**（核心）
3. **然后实现简单的参数解析**（@RequestParam）
4. **接着实现路径变量**（@PathVariable）
5. **最后实现JSON支持**（@RequestBody/@ResponseBody）

### 预计时间

- 理解文档：1-2小时
- 实现代码：8-12小时
- 测试调试：3-4小时
- **总计：12-18小时**

---

## 🚀 准备好了吗？

第六阶段会让你的框架支持Web开发，可以开发RESTful API！

理解这份文档后，告诉我，我会为你提供：
- 完整的MVC注解定义
- DispatcherServlet实现
- HandlerMapping实现
- HandlerAdapter实现
- 参数解析器
- JSON支持
- 完整的测试用例
- 可运行的Web应用示例

完成第六阶段后，你就可以用lite-spring开发真正的Web应用了！🚀

有任何疑问随时问我！💪

