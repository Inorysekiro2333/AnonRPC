package com.anon.example.consumer;

import com.anon.anonrpc.proxy.ServiceProxyFactory;
import com.anon.example.common.model.User;
import com.anon.example.common.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 简易服务消费者示例
 */
@SpringBootApplication
public class ExampleConsumerApplication {

    public static void main(String[] args) {
        // 设置服务提供者地址和端口
        System.setProperty("rpc.server.address", "localhost");
        System.setProperty("rpc.server.port", "8080");
        
        System.out.println("正在连接服务提供者...");
        
        try {
            // 获取UserService的实例对象
            UserService userService = ServiceProxyFactory.getProxy(UserService.class);
            User user = new User();
            user.setName("anon");
            
            System.out.println("发送请求: " + user.getName());
            // 调用服务
            User newUser = userService.getUser(user);
            
            if (newUser != null) {
                System.out.println("接收到响应: " + newUser.getName());
            } else {
                System.out.println("newUser is null");
            }
        } catch (Exception e) {
            System.err.println("服务调用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
