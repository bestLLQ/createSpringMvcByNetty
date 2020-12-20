package com.llq.mvc.servlet;

import com.llq.mvc.annotation.*;
import com.llq.mvc.controller.UserController;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {
    
    private List<String> classUrls = new ArrayList<>(); // 保存实例化所有类的路径：com.qfedu.annotation.MyAutowired
    private Map<String, Object> ioc = new HashMap<>(); // IOC容器 /user UserServiceImpl
    private Map<String, Object> urlHandlers = new HashMap<>(); // 存放映射地址 /user/get

    public MyDispatcherServlet() {
        super();
    }

    @Override
    // Tomcat初始化IOC
    public void init(ServletConfig config) {
        doScanPackage("com.llq"); // 扫描特殊注解的类
        doInstance(); // 实例化类
        doAutowired();  // 处理依赖
        doUrlMapping(); // 路径映射
    }

    private void doUrlMapping() {
        // ioc bean controller
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 获取带有Controller注解和Service注解的对象
            Object obj = entry.getValue();
            // 反向获取Class对象
            Class<?> clazz = obj.getClass();
            // 判断是否带有Controller注解
            if (clazz.isAnnotationPresent(Controller.class)) {
                RequestMapping mr = clazz.getAnnotation(RequestMapping.class);

                String classPath = replaceString(mr.value());

                // 获取所有方法
                Method[] methods = clazz.getMethods();

                for (Method method : methods) {
                    // 判断是否带有RequestMapping注解
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping map = method.getAnnotation(RequestMapping.class);

                        String methodPath = replaceString(map.value());

                        // 拼接类上的路径和方法上的路径作为键，带有路径的方法作为值存入urlHandlers
                        urlHandlers.put(classPath + methodPath, method);
                    }
                }
            }
        }
    }

    private void doAutowired() {
        // 此时 ioc 里已经有内容了，遍历容器
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 获取容器中所有的值(带有 MyController 或者 MyService 注解的类对象)
            Object obj = entry.getValue();

            // 通过类对象获取 Class 对象
            Class<?> clazz = obj.getClass();

            // 判断是否是 Controller 对象
            if (clazz.isAnnotationPresent(Controller.class)) {
                // 获取所有字段
                Field[] fields = clazz.getDeclaredFields();

                // 遍历所有字段
                for (Field field : fields) {
                    // 判断字段是否带有 Autowired 注解
                    if (field.isAnnotationPresent(Autowired.class)) {
                        // 获取注解对象
                        Autowired ma = field.getAnnotation(Autowired.class);
                        // 获取注解中传入的参数（ServiceImpl的路径）
                        String key = ma.value();

                        // 获取 ServiceImpl 对象
                        Object instance = ioc.get(key);

                        // 开启访问 private 权限
                        field.setAccessible(true);

                        try {
                            // Autowired依赖注入
                            // 将当前对象（Controller注解对象）的指定字段（Autowired注解字段）设置值（ServiceImpl对象）
                            field.set(obj, instance);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void doInstance() {
        // 遍历所有类路径
        for (String classUrl : classUrls) {
            try {
                Class<?> clazz = Class.forName(classUrl);

                // 判断是否带有 MyController 注解或者 MyService 注解
                if (clazz.isAnnotationPresent(Controller.class)) {
                    // 一定是控制类，实例化对象并获取注解对象
                    Object obj = clazz.newInstance();

                    RequestMapping mq = clazz.getAnnotation(RequestMapping.class);

                    // 将路径前面加上 "/"
                    String path = replaceString(mq.value());

                    // 将注释上的 value 和控制类对象作为键值存入 ioc 容器中
                    ioc.put(path, obj);

                } else if (clazz.isAnnotationPresent(Service.class)) {
                    // 一定是 service 类，实例化对象以及获取注解对象存入 ioc 容器中
                    Object obj = clazz.newInstance();

                    Service ms = clazz.getAnnotation(Service.class);

                    ioc.put(ms.value(), obj);
                }

            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void doScanPackage(String packageUrl) {
        URL url = this.getClass().getClassLoader().getResource(packageUrl.replace(".","/"));

        // Objects.requireNonNull(T obj)：判断是否为空，如果为空抛出异常
        String fileStr = Objects.requireNonNull(url).getFile();
        File file = new File(fileStr);

        String[] list = file.list();

        for (String path : list != null ? list : new String[0]) {
            File filePath = new File(fileStr + "/" + path);

            if (filePath.isDirectory()) {
                doScanPackage(packageUrl + "." + path);
            }else {
                classUrls.add(packageUrl + "." + filePath.getName().replace(".class", ""));
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置编码集
        req.setCharacterEncoding("utf-8");
        resp.setContentType("text/html; charset=utf-8");

        // 获取不带根目录的 Http 请求 URI，即 mapping 路径
        String path = req.getServletPath(); // /user/get

        // 获取方法 public void getUser(String name, String age, HttpServletResponse response)
        Method method = (Method) urlHandlers.get(path);

        // 根据请求路径在ioc中获取 controller 对象
        UserController userController = (UserController) ioc.get(replaceString(path.split("/")[1])); // /user

        Object[] args;

        try {
            // 这里做一次非空判断，否则 tomcat 加载时会报空指针异常
            if (method != null) {
                // 获取方法执行所需要的的参数参数
                args = hand(req, resp, method);
                // 执行方法
                method.invoke(userController, args);
            }

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // 获取参数
    private static Object[] hand(HttpServletRequest request, HttpServletResponse response, Method method) {
        // 拿到当前待执行的方法有哪些参数类型
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 根据参数的个数，new一个参数的数组，将方法里的所有参数赋值到args中
        Object[] args = new Object[parameterTypes.length];

        int argsIndex = 0; // 参数数组的下标
        int index = 0; // 参数数组的下标
        for (Class<?> param : parameterTypes) {
            // 判定此 Class 对象所表示的类或接口与指定的 Class 参数所表示的类或接口是否相同，或是否是其超类或超接口。
            if (ServletRequest.class.isAssignableFrom(param)) {
                args[argsIndex++] = request;
            }

            // 判断是否参数类型是否是 ServletResponse 本身或其子类
            if (ServletResponse.class.isAssignableFrom(param)) {
                // 这个参数为 HttpServletResponse，参数数组下标加1
                args[argsIndex++] = response;
            }

            // 返回表示按照声明顺序对此 Method 对象所表示方法的形参进行注释的那个数组的数组。
            // 因为一个方法的参数有多个，每个参数又有多个注解，所以返回值是一个二维数组
            // 一维数组存放参数的下标，二维数组存放参数的注解下标
            // index 就是用来存放参数的下标，用来和参数的类型一一对应
            Annotation[] paramAns = method.getParameterAnnotations()[index];

            // 判断是否有注解，比如 HttpServlet 参数就没有注解，直接跳过
            if (paramAns.length > 0) {
                // 遍历所有注解
                for (Annotation paramAn : paramAns) {
                    // 判断注解是否是 MyRequestParam 注解本身或其子类
                    if (RequestParam.class.isAssignableFrom(paramAn.getClass())) {
                        // 强转
                        RequestParam rp = (RequestParam) paramAn;
                        // 根据注解的值从request中获取前端传递过来的参数
                        String value = request.getParameter(rp.value());

                        // 因为我们从前端拿到的数据都是 json 字符串或者 json 格式，如果我们想要 int 类型的格式
                        //  就需要在这里转换一下，否则会报类型不匹配，另外如果方法上的参数类型是基础数据类型会直接报错，必须用包装类
                        // 如果方法的当前参数类型为Integer，就转换为 Integer 并添加到 args 中，否则直接添加
                        if (Integer.class.isAssignableFrom(param)) {
                            Integer integerValue;
                            try{
                                integerValue = Integer.valueOf(value);
                                args[argsIndex++] = integerValue;
                            } catch (NumberFormatException e) {
                                System.out.println("类型转换异常，请传入正确的参数");
                            }

                        } else {
                            args[argsIndex++] = value;
                        }
                    }
                }
            }
            index++;
        }
        return args;
    }

    // 给路径字符串前面拼上 "/"
    private String replaceString(String str) {
        if (! str.startsWith("/")) {
            str = "/" + str;
        }
        return str;
    }
}
