package com.anon.example.provider;


import com.anon.anonrpc.registry.LocalRegistry;
import com.anon.anonrpc.server.HttpServer;
import com.anon.anonrpc.server.VertxHttpServer;
import com.anon.example.common.service.UserService;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 简易服务提供者示例
 */
@SpringBootApplication
public class ExampleProviderApplication {

    public static void main(String[] args) {
        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);
        // 启动web服务
        HttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(8080);
    }

}
