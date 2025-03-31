package com.anon.example.consumer;

import com.anon.anonrpc.config.RpcConfig;
import com.anon.anonrpc.fault.FallbackHandler;
import com.anon.anonrpc.proxy.ServiceProxyFactory;
import com.anon.anonrpc.registry.ServiceRegistry;
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
        // System.setProperty("rpc.server.address", "localhost");
        // System.setProperty("rpc.server.port", "8080");
        String host = "localhost";
        String port = "8080";
        System.setProperty("rpc.server.address", host);
        System.setProperty("rpc.server.port", port);
        
        // 注册服务提供者
        String serviceUrl = "http://" + host + ":" + port;
        ServiceRegistry.register(serviceUrl);
        
        // 配置RPC超时和重试参数
        RpcConfig.setTimeoutMs(2000); // 2秒超时
        RpcConfig.setMaxRetries(2);   // 最多重试2次
        
        // 注册降级处理函数
        FallbackHandler.registerFallback(UserService.class, "getUser", 
            params -> {
                System.out.println("降级处理: 返回默认用户");
                User fallbackUser = new User();
                fallbackUser.setName("默认降级用户");
                return fallbackUser;
            });
        
        System.out.println("正在连接服务提供者...");
        
        // 简单测试服务器连通性
        try {
            java.net.Socket socket = new java.net.Socket(host, Integer.parseInt(port));
            System.out.println("服务提供者连接测试成功!");
            socket.close();
        } catch (Exception e) {
            System.err.println("警告: 无法连接到服务提供者: " + e.getMessage());
            System.err.println("请确认服务提供者已启动并监听" + port + "端口");
        }
        
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
