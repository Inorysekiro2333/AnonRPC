package com.anon.example.provider.controller;

import com.anon.anonrpc.registry.ServiceRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class HighPerfRegistryEndpoint extends AbstractVerticle {
    
    // 批处理大小
    private static final int BATCH_SIZE = 100;
    
    @Override
    public void start(Promise<Void> startPromise) {
        // 配置更多的事件循环线程
        vertx.createHttpServer(
                new io.vertx.core.http.HttpServerOptions()
                    .setReusePort(true)
                    .setTcpFastOpen(true)
                    .setTcpNoDelay(true)
                    .setTcpQuickAck(true)
        )
        .requestHandler(configureRouter())
        .listen(8081, ar -> {
            if (ar.succeeded()) {
                System.out.println("高性能测试端点已启动，监听端口: 8081");
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }
    
    private Router configureRouter() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        
        // 单一服务注册 - 优化版本
        router.route(HttpMethod.POST, "/api/registry/highperf").handler(ctx -> {
            String serviceType = ctx.request().getParam("serviceType");
            String serviceUrl = ctx.request().getParam("serviceUrl");
            
            // 使用非阻塞的worker线程执行，提高并发性
            vertx.executeBlocking(promise -> {
                try {
                    ServiceRegistry.register(serviceType, serviceUrl);
                    promise.complete();
                } catch (Exception e) {
                    promise.fail(e);
                }
            }, false) // 使用false表示允许并发执行
            .onSuccess(result -> {
                ctx.response()
                   .putHeader("content-type", "application/json")
                   .end("{\"status\":\"success\"}");
            })
            .onFailure(err -> {
                ctx.response()
                   .setStatusCode(500)
                   .end("{\"status\":\"error\"}");
            });
        });
        
        // 批量注册端点 - 提高JMeter测试性能
        router.route(HttpMethod.POST, "/api/registry/batch").handler(ctx -> {
            try {
                // 从JSON请求体获取批量注册数据
                io.vertx.core.json.JsonObject body = ctx.getBodyAsJson();
                io.vertx.core.json.JsonArray services = body.getJsonArray("services");
                
                if (services == null || services.isEmpty()) {
                    ctx.response()
                       .setStatusCode(400)
                       .end("{\"status\":\"error\",\"message\":\"无效的批量请求\"}");
                    return;
                }
                
                // 使用单个异步任务处理整批注册请求
                vertx.executeBlocking(promise -> {
                    try {
                        for (int i = 0; i < services.size(); i++) {
                            io.vertx.core.json.JsonObject service = services.getJsonObject(i);
                            String type = service.getString("type", "default");
                            String url = service.getString("url");
                            if (url != null && !url.isEmpty()) {
                                ServiceRegistry.register(type, url);
                            }
                        }
                        promise.complete(services.size());
                    } catch (Exception e) {
                        promise.fail(e);
                    }
                }, false)
                .onSuccess(count -> {
                    ctx.response()
                       .putHeader("content-type", "application/json")
                       .end("{\"status\":\"success\",\"count\":" + count + "}");
                })
                .onFailure(err -> {
                    ctx.response()
                       .setStatusCode(500)
                       .end("{\"status\":\"error\",\"message\":\"" + err.getMessage() + "\"}");
                });
                
            } catch (Exception e) {
                ctx.response()
                   .setStatusCode(400)
                   .end("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            }
        });
        
        return router;
    }
    
    public static void main(String[] args) {
        io.vertx.core.Vertx.vertx().deployVerticle(new HighPerfRegistryEndpoint());
    }
} 