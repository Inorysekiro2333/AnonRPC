package com.anon.example.provider;

import com.anon.example.common.model.User;
import com.anon.example.common.service.UserService;

/**
 * 用户服务实现类
 */
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(User user) {
        System.out.println("收到用户请求，用户名：" + user.getName());
        
        // 创建新对象返回，而不是直接返回原对象
        User result = new User();
        result.setName("Hello, " + user.getName());
        
        System.out.println("返回新用户对象，用户名：" + result.getName());
        return result;
    }
}
