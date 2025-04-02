package com.anon.anonrpc.server;

import com.anon.anonrpc.model.RpcRequest;
import com.anon.anonrpc.model.RpcResponse;
import com.anon.anonrpc.registry.LocalRegistry;
import com.anon.anonrpc.serializer.JdkSerializer;
import com.anon.anonrpc.serializer.Serializer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步HTTP请求处理器 - 基于Vert.x事件总线 + CompletableFuture
 */
public class AsyncHttpServerHandler implements Handler<HttpServerRequest> {
    
    // 创建线程池，用于执行计算密集型任务
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    
    // 事件总线
    private final EventBus eventBus;
    
    // 序列化器缓存
    private static final ConcurrentHashMap<String, Serializer> SERIALIZER_CACHE = new ConcurrentHashMap<>();
    
    // 服务实例缓存
    private static final ConcurrentHashMap<String, Object> SERVICE_INSTANCE_CACHE = new ConcurrentHashMap<>();
    
    public AsyncHttpServerHandler(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        
        // 注册事件总线处理器，用于处理RPC请求
        eventBus.consumer("rpc.request", message -> {
            RpcRequest rpcRequest = (RpcRequest) message.body();
            
            // 异步执行RPC调用
            CompletableFuture.supplyAsync(() -> {
                try {
                    // 获取服务实现类
                    String serviceName = rpcRequest.getServiceName();
                    Object serviceInstance = SERVICE_INSTANCE_CACHE.computeIfAbsent(
                        serviceName,
                        key -> {
                            try {
                                Class<?> implClass = LocalRegistry.get(key);
                                return implClass.getDeclaredConstructor().newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException("创建服务实例失败: " + key, e);
                            }
                        }
                    );
                    
                    // 获取方法
                    Class<?> serviceClass = serviceInstance.getClass();
                    Method method = serviceClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                    
                    // 调用方法
                    Object result = method.invoke(serviceInstance, rpcRequest.getArgs());
                    
                    // 构建响应
                    return RpcResponse.builder()
                        .data(result)
                        .dataType(method.getReturnType())
                        .message("success")
                        .build();
                } catch (Exception e) {
                    return RpcResponse.builder()
                        .exception(e)
                        .message("error: " + e.getMessage())
                        .build();
                }
            }, EXECUTOR).whenComplete((response, error) -> {
                if (error != null) {
                    message.reply(RpcResponse.builder()
                        .message("处理请求时发生错误: " + error.getMessage())
                        .exception(new Exception(error))
                        .build());
                } else {
                    message.reply(response);
                }
            });
        });
    }

    @Override
    public void handle(HttpServerRequest request) {
        // 处理浏览器直接访问
        if (request.method() == io.vertx.core.http.HttpMethod.GET) {
            request.response()
                   .putHeader("content-type", "text/html; charset=UTF-8")
                   .end("<html><body>" +
                        "<h1>RPC服务器正在运行</h1>" +
                        "<p>这是一个RPC服务端点，不支持直接浏览器访问。</p>" +
                        "<p>请使用正确的RPC客户端访问此服务。</p>" +
                        "</body></html>");
            return;
        }
        
        // 异步处理HTTP请求体
        request.body().onSuccess(buffer -> {
            try {
                // 获取序列化器
                Serializer serializer = SERIALIZER_CACHE.computeIfAbsent("jdk", k -> new JdkSerializer());
                
                // 反序列化请求
                byte[] bytes = buffer.getBytes();
                
                // 如果请求为空，返回错误
                if (bytes == null || bytes.length == 0) {
                    doErrorResponse(request, "rpcRequest is null", serializer);
                    return;
                }
                
                // 异步反序列化和处理
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return serializer.deserialize(bytes, RpcRequest.class);
                    } catch (IOException e) {
                        throw new RuntimeException("反序列化请求失败", e);
                    }
                }, EXECUTOR).thenCompose(rpcRequest -> {
                    // 通过事件总线发送请求并等待响应
                    CompletableFuture<RpcResponse> future = new CompletableFuture<>();
                    eventBus.request("rpc.request", rpcRequest, reply -> {
                        if (reply.succeeded()) {
                            future.complete((RpcResponse) reply.result().body());
                        } else {
                            future.completeExceptionally(reply.cause());
                        }
                    });
                    return future;
                }).whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        doErrorResponse(request, "处理请求失败: " + throwable.getMessage(), serializer);
                    } else {
                        doResponse(request, response, serializer);
                    }
                });
            } catch (Exception e) {
                doErrorResponse(request, "处理请求异常: " + e.getMessage(), new JdkSerializer());
            }
        }).onFailure(err -> {
            doErrorResponse(request, "读取请求体失败: " + err.getMessage(), new JdkSerializer());
        });
    }
    
    void doErrorResponse(HttpServerRequest request, String message, Serializer serializer) {
        RpcResponse rpcResponse = RpcResponse.builder()
                .message(message)
                .build();
        doResponse(request, rpcResponse, serializer);
    }
    
    void doResponse(HttpServerRequest request, RpcResponse rpcResponse, Serializer serializer) {
        HttpServerResponse httpServerResponse = request.response()
                .putHeader("content-type", "application/json");
        try {
            // 异步序列化
            CompletableFuture.supplyAsync(() -> {
                try {
                    return serializer.serialize(rpcResponse);
                } catch (IOException e) {
                    throw new RuntimeException("序列化响应失败", e);
                }
            }, EXECUTOR).whenComplete((serialized, error) -> {
                if (error != null) {
                    System.err.println("序列化响应时出错: " + error.getMessage());
                    error.printStackTrace();
                    
                    // 返回错误信息而不是空响应
                    try {
                        RpcResponse errorResponse = RpcResponse.builder()
                            .message("服务器序列化错误: " + error.getMessage())
                            .build();
                        byte[] errorBytes = serializer.serialize(errorResponse);
                        httpServerResponse.end(Buffer.buffer(errorBytes));
                    } catch (IOException ex) {
                        // 如果连错误响应都无法序列化，发送纯文本错误
                        httpServerResponse.putHeader("content-type", "text/plain")
                                        .end("服务器序列化错误");
                    }
                } else {
                    System.out.println("响应序列化成功，数据大小: " + serialized.length + " 字节");
                    httpServerResponse.end(Buffer.buffer(serialized));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            httpServerResponse.putHeader("content-type", "text/plain")
                             .end("服务器内部错误: " + e.getMessage());
        }
    }
} 