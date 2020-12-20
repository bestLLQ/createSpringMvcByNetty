package com.llq.mvc.service.impl;

import com.llq.mvc.annotation.Service;
import com.llq.mvc.service.UserService;

/**
 * @author Buffer
 * @date 2020/8/4 21:49
 * @Description 接口实现类
 */
@Service(value = "UserServiceImpl")
public class UserServiceImpl implements UserService {

    @Override
    public String getUser(String name, String age) {
        return name + age;
    }
}