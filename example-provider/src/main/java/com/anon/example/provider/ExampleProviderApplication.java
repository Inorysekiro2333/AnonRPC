package com.anon.example.provider;

import com.anon.anonrpc.registry.LocalRegistry;
import com.anon.anonrpc.registry.ServiceRegistry;
import com.anon.anonrpc.server.AsyncVertxHttpServer;
import com.anon.anonrpc.server.HttpServer;
import com.anon.example.common.service.UserService;
import com.anon.example.provider.controller.HighPerfRegistryEndpoint;
import io.vertx.core.Vertx;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * 异步RPC服务提供者示例
 */
@SpringBootApplication
@EnableWebMvc
public class ExampleProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleProviderApplication.class, args);
        
        // 注册服务
        String serviceUrl = "http://localhost:8080";
        ServiceRegistry.register(serviceUrl);
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);
        
        // 启动异步RPC服务器 - 使用不同端口
        HttpServer httpServer = new AsyncVertxHttpServer();
        httpServer.doStart(8090); // 改为8090端口
        
        // 部署高性能测试端点
        Vertx.vertx().deployVerticle(new HighPerfRegistryEndpoint());
    }
}
