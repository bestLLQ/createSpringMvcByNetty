package com.llq.mvc.controller;

import com.alibaba.fastjson.JSONObject;
import com.llq.mvc.annotation.Autowired;
import com.llq.mvc.annotation.Controller;
import com.llq.mvc.annotation.RequestMapping;
import com.llq.mvc.annotation.RequestParam;
import com.llq.mvc.service.UserService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Buffer
 * @date 2020/8/4 21:46
 * @Description 用户控制类
 */
@Controller
@RequestMapping("user")
public class UserController {

    @Autowired(value = "UserServiceImpl")
    private UserService userService;

    @RequestMapping("/getUser")
    public void getUser(@RequestParam("username") String username, @RequestParam("password") String password,
                        HttpServletResponse response) {

        try {
            response.getWriter().write(userService.getUser(username, password));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/getJson")
    public void getJson(@RequestParam("name") String name, @RequestParam("age") Integer age,
                       HttpServletResponse response) {

        System.out.println("从前端获取到的参数：" + name + " : " + age);

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("name", name);
        jsonObject.put("age", age);

        // 声明文本输出流
        PrintWriter writer;

        // 实例化对象，执行写操作，刷新，关闭
        try {
            writer = response.getWriter();
            writer.print(jsonObject.toJSONString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}