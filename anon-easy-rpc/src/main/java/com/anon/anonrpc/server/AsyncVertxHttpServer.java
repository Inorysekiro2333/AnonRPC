package com.anon.anonrpc.server;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * 基于Vert.x的异步HTTP服务器
 */
public class AsyncVertxHttpServer implements HttpServer {
    
    private Vertx vertx;
    
    /**
     * 启动异步服务器
     */
    @Override
    public void doStart(int port) {
        // 配置Vert.x，优化事件循环和工作线程
        VertxOptions options = new VertxOptions()
            .setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2)
            .setWorkerPoolSize(50)
            .setInternalBlockingPoolSize(50);
        
        // 创建Vert.x实例
        vertx = Vertx.vertx(options);
        
        // 创建HTTP服务器
        io.vertx.core.http.HttpServer server = vertx.createHttpServer();
        
        // 设置异步请求处理器
        server.requestHandler(new AsyncHttpServerHandler(vertx));
        
        // 启动HTTP服务器
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("异步RPC服务器已启动，监听端口: " + port);
            } else {
                System.err.println("启动服务器失败: " + result.cause());
            }
        });
    }
} 