package com.anon.example.provider;


import com.anon.anonrpc.registry.LocalRegistry;
import com.anon.anonrpc.server.HttpServer;
import com.anon.anonrpc.server.VertxHttpServer;
import com.anon.example.common.service.UserService;
import com.anon.anonrpc.registry.ServiceRegistry;


/**
 * 简易服务提供者示例
 */
public class ExampleProviderApplication {

    public static void main(String[] args) {
        // 注册服务
        String serviceUrl = "http://localhost:8080"; // 假设服务提供者的URL
        ServiceRegistry.register(serviceUrl);
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);
        // 启动web服务
        HttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(8080);
    }

}
